package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.entity.Pet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Assistente simulado: casa palavras-chave da pergunta com um modelo de resposta. */
@Component
public class AssistenteIASimulado {

    // %1$s = nome do pet, %2$s = espécie
    private record Regra(List<String> palavras, String modelo) {}

    private static final List<Regra> REGRAS = List.of(
            new Regra(List.of("vacin"),
                    "Para %1$s (%2$s), é essencial manter o calendário vacinal em dia. As vacinas principais variam "
                            + "por espécie e estilo de vida. Recomendo consultar seu veterinário para montar o protocolo "
                            + "ideal. Em cães, as vacinas V8/V10 e antirrábica são fundamentais; em gatos, V4/V5 e FeLV."),
            new Regra(List.of("aliment", "ração", "comida"),
                    "A nutrição de %1$s deve ser adequada à espécie %2$s, faixa etária e condição corporal. "
                            + "Prefira rações de qualidade com proteína como primeiro ingrediente. Evite alimentos "
                            + "humanos como uva, cebola, chocolate e xilitol, que são tóxicos para pets."),
            new Regra(List.of("comportamento", "agitad", "agressiv"),
                    "Mudanças de comportamento em %1$s podem indicar estresse, dor, doença ou ansiedade. "
                            + "Observe padrões como perda de apetite, letargia ou isolamento. Se os sintomas persistirem "
                            + "por mais de 48h, procure atendimento veterinário para avaliação clínica completa."),
            new Regra(List.of("banho", "higiene", "pelo", "tosa"),
                    "A higiene de %1$s (%2$s) é fundamental para saúde e bem-estar. A frequência do banho varia por "
                            + "raça e pelagem. Use sempre produtos específicos para pets — produtos humanos podem alterar "
                            + "o pH da pele. Escovações regulares previnem nós e parasitas."),
            new Regra(List.of("doença", "sintoma", "vômit", "diarr"),
                    "Se %1$s apresenta sintomas como vômito, diarreia, letargia ou perda de apetite, "
                            + "procure atendimento veterinário imediatamente. Não administre medicamentos humanos "
                            + "sem prescrição — muitos são tóxicos para pets. Hidratação adequada é essencial."),
            new Regra(List.of("parasit", "pulga", "carrapath", "verme"),
                    "O controle de parasitas em %1$s deve ser feito de forma preventiva e contínua. "
                            + "Antipulgas, anticarrapatos e vermífugos devem ser aplicados regularmente conforme "
                            + "orientação veterinária. A prevenção protege tanto o pet quanto a família."),
            new Regra(List.of("castração", "castrar", "esteriliz"),
                    "A castração de %1$s traz benefícios como prevenção de doenças reprodutivas, redução "
                            + "de comportamentos indesejados e controle populacional. O momento ideal varia por "
                            + "espécie e raça. Consulte seu veterinário para avaliar o melhor momento para %1$s.")
    );

    private static final String RESPOSTA_PADRAO =
            "Olá! Estou aqui para ajudar com o cuidado de %1$s (%2$s). Posso orientar sobre alimentação, "
                    + "vacinação, higiene, comportamento e saúde preventiva. Para diagnósticos e tratamentos, "
                    + "sempre consulte um médico veterinário. Como posso ajudar melhor hoje?";

    public String responder(String pergunta, Pet pet) {
        String texto = pergunta.toLowerCase(Locale.ROOT);
        String modelo = REGRAS.stream()
                .filter(r -> r.palavras().stream().anyMatch(texto::contains))
                .map(Regra::modelo)
                .findFirst()
                .orElse(RESPOSTA_PADRAO);
        return modelo.formatted(pet.getNome(), pet.getEspecie());
    }
}