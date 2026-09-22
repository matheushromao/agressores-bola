package com.hmz.agressores_da_bola.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmz.agressores_da_bola.domain.model.DadosPelada;
import com.hmz.agressores_da_bola.domain.model.enums.TipoCampo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Agendamento de uma pelada. O organizador não vem no corpo: "
        + "é sempre o usuário do token.")
public record PeladaRequest(

        @Schema(description = "Nome da pelada", example = "Pelada de quinta")
        @NotBlank(message = "O nome da pelada é obrigatório")
        @Size(min = 3, max = 100, message = "O nome da pelada deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "Observações para os convidados", example = "Levar colete e camisa branca")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "Data do jogo; não pode estar no passado", example = "2026-10-15")
        @NotNull(message = "A data da pelada é obrigatória")
        @FutureOrPresent(message = "A data da pelada não pode estar no passado")
        LocalDate data,

        @Schema(description = "Horário de início", example = "19:00:00")
        @NotNull(message = "O horário de início é obrigatório")
        LocalTime horaInicio,

        @Schema(description = "Horário de término; precisa ser posterior ao início", example = "21:00:00")
        @NotNull(message = "O horário de término é obrigatório")
        LocalTime horaFim,

        @Schema(description = "Nome da quadra ou do campo", example = "Arena Vila Progresso")
        @NotBlank(message = "O nome do local é obrigatório")
        @Size(max = 120, message = "O nome do local deve ter no máximo 120 caracteres")
        String localNome,

        @Schema(description = "Endereço do local", example = "Rua das Palmeiras, 120")
        @NotBlank(message = "O endereço é obrigatório")
        @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
        String endereco,

        @Schema(description = "Cidade", example = "Sorocaba")
        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres")
        String cidade,

        @Schema(description = "Sigla do estado, com 2 letras", example = "SP")
        @NotBlank(message = "O estado é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado deve ser a sigla com 2 letras, por exemplo SP")
        String estado,

        @Schema(description = "Tipo de campo")
        @NotNull(message = "O tipo de campo é obrigatório")
        TipoCampo tipoCampo,

        @Schema(description = "Limite de vagas; a partir daí a escalação vira lista de espera",
                example = "14")
        @NotNull(message = "O número máximo de participantes é obrigatório")
        @Min(value = 2, message = "A pelada precisa de no mínimo 2 participantes")
        @Max(value = 50, message = "A pelada aceita no máximo 50 participantes")
        Integer maxParticipantes,

        @Schema(description = "Rateio por jogador, em reais", example = "25.00")
        @DecimalMin(value = "0.0", message = "O valor por jogador não pode ser negativo")
        @DecimalMax(value = "9999.99", message = "O valor por jogador deve ser menor que 10.000,00")
        BigDecimal valorPorJogador

) {
    // O organizador não vem no corpo: é sempre o usuário do token.

    public DadosPelada paraDados() {
        return new DadosPelada(nome, descricao, data, horaInicio, horaFim, localNome,
                endereco, cidade, estado, tipoCampo, maxParticipantes, valorPorJogador);
    }

    /**
     * Validação de campos cruzados: o Bean Validation resolve cada campo
     * isoladamente, então a relação entre início e fim mora aqui.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "O horário de término deve ser posterior ao horário de início")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }
}
