package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.pelada.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.application.usuario.UsuarioMapper;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.hmz.agressores_da_bola.domain.model.enums.Descritivel.descricaoDe;

/**
 * Monta as respostas da pelada. Criar e alterar a entidade não passa mais
 * por aqui: isso é comportamento da própria {@link Pelada}. Reaproveita o
 * {@link UsuarioMapper} para o resumo do jogador em vez de duplicar a
 * montagem do usuário aqui.
 */
@Component
@RequiredArgsConstructor
public class PeladaMapper {

    private final UsuarioMapper usuarioMapper;

    public PeladaResponse toResponse(Pelada pelada) {
        return new PeladaResponse(
                pelada.getId(),
                pelada.getNome(),
                pelada.getDescricao(),
                pelada.getData(),
                pelada.getHoraInicio(),
                pelada.getHoraFim(),
                pelada.getLocalNome(),
                pelada.getEndereco(),
                pelada.getCidade(),
                pelada.getEstado(),
                pelada.getTipoCampo(),
                descricaoDe(pelada.getTipoCampo()),
                pelada.getMaxParticipantes(),
                pelada.totalConfirmados(),
                pelada.vagasRestantes(),
                pelada.getValorPorJogador(),
                pelada.getStatus(),
                descricaoDe(pelada.getStatus()),
                usuarioMapper.toResumoResponse(pelada.getOrganizador()),
                toParticipantesResponse(pelada)
        );
    }

    public PeladaResumoResponse toResumoResponse(Pelada pelada) {
        return new PeladaResumoResponse(
                pelada.getId(),
                pelada.getNome(),
                pelada.getData(),
                pelada.getHoraInicio(),
                pelada.getHoraFim(),
                pelada.getLocalNome(),
                pelada.getCidade(),
                pelada.getEstado(),
                pelada.getTipoCampo(),
                descricaoDe(pelada.getTipoCampo()),
                pelada.getStatus(),
                descricaoDe(pelada.getStatus()),
                pelada.getMaxParticipantes(),
                pelada.totalConfirmados(),
                pelada.vagasRestantes(),
                pelada.getValorPorJogador(),
                pelada.getOrganizador() != null ? pelada.getOrganizador().getNickname() : null
        );
    }

    public ParticipanteResponse toParticipanteResponse(ParticipacaoPelada participacao) {
        return new ParticipanteResponse(
                participacao.getId(),
                usuarioMapper.toResumoResponse(participacao.getUsuario()),
                participacao.getStatus(),
                descricaoDe(participacao.getStatus()),
                participacao.getDataInscricao()
        );
    }

    private List<ParticipanteResponse> toParticipantesResponse(Pelada pelada) {
        return pelada.escalacaoPorOrdemDeInscricao().stream()
                .map(this::toParticipanteResponse)
                .toList();
    }
}
