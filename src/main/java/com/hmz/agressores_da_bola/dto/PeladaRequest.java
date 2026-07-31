package com.hmz.agressores_da_bola.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
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

public record PeladaRequest(

        @NotBlank(message = "O nome da pelada é obrigatório")
        @Size(min = 3, max = 100, message = "O nome da pelada deve ter entre 3 e 100 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "A data da pelada é obrigatória")
        @FutureOrPresent(message = "A data da pelada não pode estar no passado")
        LocalDate data,

        @NotNull(message = "O horário de início é obrigatório")
        LocalTime horaInicio,

        @NotNull(message = "O horário de término é obrigatório")
        LocalTime horaFim,

        @NotBlank(message = "O nome do local é obrigatório")
        @Size(max = 120, message = "O nome do local deve ter no máximo 120 caracteres")
        String localNome,

        @NotBlank(message = "O endereço é obrigatório")
        @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
        String endereco,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres")
        String cidade,

        @NotBlank(message = "O estado é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado deve ser a sigla com 2 letras, por exemplo SP")
        String estado,

        @NotNull(message = "O tipo de campo é obrigatório")
        TipoCampo tipoCampo,

        @NotNull(message = "O número máximo de participantes é obrigatório")
        @Min(value = 2, message = "A pelada precisa de no mínimo 2 participantes")
        @Max(value = 50, message = "A pelada aceita no máximo 50 participantes")
        Integer maxParticipantes,

        @DecimalMin(value = "0.0", message = "O valor por jogador não pode ser negativo")
        @DecimalMax(value = "9999.99", message = "O valor por jogador deve ser menor que 10.000,00")
        BigDecimal valorPorJogador,

        @NotNull(message = "O organizador é obrigatório")
        Long organizadorId

) {

    /**
     * Validação de campos cruzados: o Bean Validation resolve cada campo
     * isoladamente, então a relação entre início e fim mora aqui.
     */
    @JsonIgnore
    @AssertTrue(message = "O horário de término deve ser posterior ao horário de início")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }
}
