package com.hmz.agressores_da_bola;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * A regra de dependência da Clean Architecture, verificada a cada build: as
 * setas só apontam para dentro. {@code web} e {@code infrastructure} dependem
 * de {@code application}, que depende de {@code domain}, que não depende de
 * ninguém — nem do Spring.
 */
@AnalyzeClasses(packages = "com.hmz.agressores_da_bola", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String DOMAIN = "..agressores_da_bola.domain..";
    private static final String APPLICATION = "..agressores_da_bola.application..";
    private static final String INFRASTRUCTURE = "..agressores_da_bola.infrastructure..";
    private static final String WEB = "..agressores_da_bola.web..";

    @ArchTest
    static final ArchRule dominioNaoConheceAsOutrasCamadas = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, WEB)
            .because("o domínio é o centro: as regras de negócio não podem mudar por causa de HTTP ou banco");

    /**
     * JPA e Hibernate ficam de fora de propósito: as entidades de domínio são
     * também as entidades persistidas, um compromisso pragmático documentado
     * no passo 7.
     */
    @ArchTest
    static final ArchRule dominioNaoDependeDoSpring = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .because("o domínio precisa ser testável sem subir contexto nenhum");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceWebNemInfraestrutura = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, WEB)
            .because("os casos de uso falam com o mundo só pelas portas em application.port");

    @ArchTest
    static final ArchRule aplicacaoNaoUsaJpaDiretamente = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data.jpa..", "jakarta.persistence..")
            .because("consultas e especificações JPA são detalhe do adapter de persistência");

    @ArchTest
    static final ArchRule infraestruturaNaoConheceWeb = noClasses()
            .that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(WEB);

    @ArchTest
    static final ArchRule webNaoConheceInfraestrutura = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("o controller conversa com os casos de uso, nunca com o banco");

    @ArchTest
    static final ArchRule controllersDependemDoContratoENaoDaImplementacao = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("ServiceImpl")
            .because("Dependency Inversion: o controller depende da interface do caso de uso");
}
