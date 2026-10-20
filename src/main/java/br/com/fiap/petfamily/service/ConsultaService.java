package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.config.InvalidaCaches;
import br.com.fiap.petfamily.dto.request.AgendarConsultaRequest;
import br.com.fiap.petfamily.dto.request.RealizarConsultaRequest;
import br.com.fiap.petfamily.dto.response.ConsultaResponse;
import br.com.fiap.petfamily.entity.Consulta;
import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import br.com.fiap.petfamily.entity.ConsultaSlot;
import br.com.fiap.petfamily.entity.Pet;
import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.exception.ConflitoOperacaoException;
import br.com.fiap.petfamily.exception.OperacaoInvalidaException;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.ConsultaRepository;
import br.com.fiap.petfamily.repository.ConsultaSlotRepository;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import br.com.fiap.petfamily.security.UsuarioAtual;
import br.com.fiap.petfamily.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private static final String CACHE_CONSULTAS = "consultas";
    private static final String CACHE_DASHBOARD = "dashboard";
    private static final String CACHE_PETS = "pets";

    private final ConsultaRepository consultaRepository;
    private final ConsultaSlotRepository consultaSlotRepository;
    private final PetService petService;
    private final SecurityUtils securityUtils;
    private final UsuarioAtual  usuarioAtual;

    @Transactional
    @InvalidaCaches
    public ConsultaResponse agendar(AgendarConsultaRequest request) {
        securityUtils.getTutorIdAutenticadoOuFalha();
        Pet pet = petService.findById(request.getPetId());
        securityUtils.exigirTutorDono(pet.getTutor().getId(), "O pet informado não pertence ao tutor autenticado.");

        // Normaliza para minutos: 10:00:00 e 10:00:30 não podem virar "dois horários".
        LocalTime horario = request.getHorario().truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime momentoAgendado = LocalDateTime.of(request.getData(), horario);
        if (!momentoAgendado.isAfter(LocalDateTime.now())) {
            throw new OperacaoInvalidaException("A consulta deve ser marcada para uma data e horário futuros.");
        }

        // Checagem antecipada (mensagem clara, sem depender da exceção do banco).
        // A UNIQUE(data, horario) continua sendo a garantia final contra concorrência.
        if (consultaSlotRepository.existsByDataAndHorario(request.getData(), horario)) {
            throw new ConflitoOperacaoException(
                    "Já existe uma consulta agendada para " + request.getData() + " às " + horario + ".");
        }

        Consulta consulta = consultaRepository.save(Consulta.builder()
                .data(request.getData())
                .horario(horario)
                .tipoConsulta(request.getTipoConsulta())
                .status(StatusConsulta.AGENDADA)
                .observacoes(request.getObservacoes())
                .pet(pet)
                .build());

        try {
            consultaSlotRepository.saveAndFlush(ConsultaSlot.builder()
                    .data(request.getData())
                    .horario(horario)
                    .consultaId(consulta.getId())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoOperacaoException(
                    "Já existe uma consulta agendada para " + request.getData() + " às " + horario + ".");
        }

        return toResponse(consulta);
    }

    @Transactional
    @InvalidaCaches
    public ConsultaResponse realizar(Long id, RealizarConsultaRequest request) {
        Consulta consulta = findById(id);
        Usuario veterinario = usuarioAtual.entidade();
        int atualizados = consultaRepository.realizarSeAgendada(
                id, request.getObservacoes(), LocalDateTime.now(), veterinario);
        if (atualizados == 0) {
            throw new ConflitoOperacaoException(
                    "Consulta não pode ser realizada: status atual é " + consulta.getStatus() + ".");
        }
        return toResponse(findById(id));
    }

    @Transactional
    @InvalidaCaches
    public ConsultaResponse cancelar(Long id) {
        Consulta consulta = findById(id);
        securityUtils.exigirTutorDono(consulta.getPet().getTutor().getId(), "Esta consulta não pertence ao tutor autenticado.");

        int atualizados = consultaRepository.cancelarSeAgendada(id);
        if (atualizados == 0) {
            throw new ConflitoOperacaoException(
                    "Consulta não pode ser cancelada: status atual é " + consulta.getStatus() + ".");
        }
        consultaSlotRepository.deleteByConsultaId(id);
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listar(StatusConsulta status, Pageable pageable) {
        UsuarioPrincipal principal = securityUtils.getUsuarioAutenticado();
        if (principal.isTutor()) {
            Page<Consulta> page = status != null
                    ? consultaRepository.findByPetTutorIdAndStatus(principal.getTutorId(), status, pageable)
                    : consultaRepository.findByPetTutorId(principal.getTutorId(), pageable);
            return page.map(this::toResponse);
        }
        Page<Consulta> page = status != null
                ? consultaRepository.findByStatus(status, pageable)
                : consultaRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConsultaResponse buscarPorId(Long id) {
        Consulta consulta = findById(id);
        securityUtils.exigirTutorDono(consulta.getPet().getTutor().getId(), "Esta consulta não pertence ao tutor autenticado.");
        return toResponse(consulta);
    }

    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listarFuturas(Pageable pageable) {
        return consultaRepository
                .findByStatusAndDataGreaterThanEqual(StatusConsulta.AGENDADA, LocalDate.now(), pageable)
                .map(this::toResponse);
    }

    Consulta findById(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com id: " + id));
    }

    private ConsultaResponse toResponse(Consulta c) {
        return ConsultaResponse.builder()
                .id(c.getId())
                .data(c.getData())
                .horario(c.getHorario())
                .tipoConsulta(c.getTipoConsulta())
                .status(c.getStatus())
                .observacoes(c.getObservacoes())
                .petId(c.getPet().getId())
                .petNome(c.getPet().getNome())
                .tutorId(c.getPet().getTutor().getId())
                .tutorNome(c.getPet().getTutor().getNome())
                .criadoEm(c.getCriadoEm())
                .realizadaEm(c.getRealizadaEm())
                .realizadaPorNome(c.getRealizadaPor() != null ? c.getRealizadaPor().getNome() : null)
                .build();
    }
}