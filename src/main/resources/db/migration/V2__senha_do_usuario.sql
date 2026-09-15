-- Senha do usuário para o login (Etapa 3: Spring Security + JWT).
--
-- Guarda o hash do DelegatingPasswordEncoder ("{bcrypt}" + 60 caracteres),
-- nunca a senha em texto puro.
--
-- NOT NULL sem valor padrão só funciona porque as bases existentes estavam
-- vazias quando esta migration foi criada. Numa base com usuários, seria
-- preciso adicionar a coluna como NULL, preencher com um hash provisório e
-- forçar a troca de senha, e só então aplicar o NOT NULL.

ALTER TABLE tb_usuarios
    ADD COLUMN senha VARCHAR(100) NOT NULL AFTER email;
