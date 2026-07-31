package com.hmz.agressores_da_bola.mapper;

import com.hmz.agressores_da_bola.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.dto.PeladaRequest;
import com.hmz.agressores_da_bola.dto.PeladaResponse;
import com.hmz.agressores_da_bola.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Conversão entre a entidade Pelada e seus DTOs. Reaproveita o
 * {@link UsuarioMapper} para o resumo do jogador em vez de duplicar a
 * montagem do usuário aqui.
 */
@Component
@RequiredArgsConstructor
public class PeladaMapper {

    private final UsuarioMapper usuarioMapper;

    public Pelada toEntity(PeladaRequest request, Usuario organizador) {
        return Pelada.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .data(request.data())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .localNome(request.localNome())
                .endereco(request.endereco())
                .cidade(request.cidade())
                .estado(request.estado().toUpperCase())
                .tipoCampo(request.tipoCampo())
                .maxParticipantes(request.maxParticipantes())
                .valorPorJogador(request.valorPorJogador())
                .status(StatusPelada.AGENDADA)
                .organizador(organizador)
                .build();
    }

    /**
     * Atualiza os dados editáveis. Organizador e status ficam de fora de
     * propósito: têm regras próprias e endpoints próprios.
     */
    public void copyToEntity(PeladaRequest request, Pelada pelada) {
        pelada.setNome(request.nome());
        pelada.setDescricao(request.descricao());
        pelada.setData(request.data());
        pelada.setHoraInicio(request.horaInicio());
        pelada.setHoraFim(request.horaFim());
        pelada.setLocalNome(request.localNome());
        pelada.setEndereco(request.endereco());
        pelada.setCidade(request.cidade());
        pelada.setEstado(request.estado().toUpperCase());
        pelada.setTipoCampo(request.tipoCampo());
        pelada.setMaxParticipantes(request.maxParticipantes());
        pelada.setValorPorJogador(request.valorPorJogador());
    }

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
                pelada.getTipoCampo() != null ? pelada.getTipoCampo().getDescricao() : null,
                pelada.getMaxParticipantes(),
                pelada.totalConfirmados(),
                pelada.vagasRestantes(),
                pelada.getValorPorJogador(),
                pelada.getStatus(),
                pelada.getStatus() != null ? pelada.getStatus().getDescricao() : null,
                usuarioMapper.toResumoResponse(pelada.getOrganizador()),
                toParticipantesResponse(pelada.getParticipacoes())
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
                pelada.getTipoCampo() != null ? pelada.getTipoCampo().getDescricao() : null,
                pelada.getStatus(),
                pelada.getStatus() != null ? pelada.getStatus().getDescricao() : null,
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
                participacao.getStatus() != null ? participacao.getStatus().getDescricao() : null,
                participacao.getDataInscricao()
        );
    }

    public List<ParticipanteResponse> toParticipantesResponse(List<ParticipacaoPelada> participacoes) {
        return participacoes.stream()
                .sorted(Comparator.comparing(
                        ParticipacaoPelada::getDataInscricao,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toParticipanteResponse)
                .toList();
    }
}
