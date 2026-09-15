-- Schema inicial, extraído do mapeamento JPA (Hibernate 7, MySQLDialect).
--
-- A partir daqui o schema pertence ao Flyway: o Hibernate roda com
-- ddl-auto: validate e só confere se as entidades batem com o banco.
-- Nunca edite uma migration já aplicada; crie a próxima V{n}__descricao.sql.
--
-- Colunas de enum são ENUM(...) nativas. Para renomear ou remover valores,
-- use três passos na mesma migration:
--   1. MODIFY para o superconjunto (valores antigos + novos)
--   2. UPDATE com o de-para
--   3. MODIFY para a lista final

CREATE TABLE tb_usuarios (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nome_completo  VARCHAR(120) NOT NULL,
    nickname       VARCHAR(30)  NOT NULL,
    descricao      VARCHAR(500),
    numero_celular VARCHAR(20)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    idade          INTEGER      NOT NULL,
    posicao        ENUM ('ALA','AMADOR','FIXO','GOLEIRO','PIVO') NOT NULL,
    nacionalidade  VARCHAR(60)  NOT NULL,
    estrelas       DECIMAL(2,1),
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_nickname UNIQUE (nickname),
    CONSTRAINT uk_usuario_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_peladas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    nome              VARCHAR(100)  NOT NULL,
    descricao         VARCHAR(500),
    data              DATE          NOT NULL,
    hora_inicio       TIME(0)       NOT NULL,
    hora_fim          TIME(0)       NOT NULL,
    local_nome        VARCHAR(120)  NOT NULL,
    endereco          VARCHAR(200)  NOT NULL,
    cidade            VARCHAR(80)   NOT NULL,
    estado            VARCHAR(2)    NOT NULL,
    tipo_campo        ENUM ('FUTSAL','SOCIETY') NOT NULL,
    max_participantes INTEGER       NOT NULL,
    valor_por_jogador DECIMAL(10,2),
    status            ENUM ('AGENDADA','CANCELADA','CONFIRMADA','EM_ANDAMENTO','FINALIZADA') NOT NULL,
    organizador_id    BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pelada_organizador FOREIGN KEY (organizador_id) REFERENCES tb_usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_participacoes_pelada (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    pelada_id      BIGINT      NOT NULL,
    usuario_id     BIGINT      NOT NULL,
    status         ENUM ('CONFIRMADO','CONVIDADO','LISTA_DE_ESPERA','RECUSADO') NOT NULL,
    data_inscricao DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_participacao_pelada_usuario UNIQUE (pelada_id, usuario_id),
    CONSTRAINT fk_participacao_pelada  FOREIGN KEY (pelada_id)  REFERENCES tb_peladas (id),
    CONSTRAINT fk_participacao_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tb_estatisticas_partida (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    participacao_id  BIGINT      NOT NULL,
    posicao_jogada   ENUM ('ALA','AMADOR','FIXO','GOLEIRO','PIVO') NOT NULL,
    gols             INTEGER     NOT NULL,
    assistencias     INTEGER     NOT NULL,
    desarmes         INTEGER     NOT NULL,
    defesas          INTEGER     NOT NULL,
    defesas_dificeis INTEGER     NOT NULL,
    registrada_em    DATETIME(6) NOT NULL,
    atualizada_em    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_estatistica_participacao UNIQUE (participacao_id),
    CONSTRAINT fk_estatistica_participacao FOREIGN KEY (participacao_id) REFERENCES tb_participacoes_pelada (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
