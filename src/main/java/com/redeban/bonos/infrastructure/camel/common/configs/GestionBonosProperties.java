package com.redeban.bonos.infrastructure.camel.common.configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.Map;

@ConfigMapping(prefix = "bonos")
public interface GestionBonosProperties {

    @WithName("as400.store-procedure")
    String storedProcedure();

    Codigo codigo();

    Dispositivo dispositivo();

    Tipo tipo();

    @WithName("numero-bin")
    NumeroBin numeroBin();

    Map<String, String> documentos();

    @WithName("codigo.respuesta")
    CodigoRespuesta codigoRespuesta();

    Descripcion descripcion();

    interface Codigo {
        String aplicacion();

        @WithName("switch")
        String switchCode();

        Novedad novedad();

        Transaccion transaccion();

        @WithName("E01")
        String e01();

        @WithName("E03")
        String e03();

        @WithName("E08")
        String e08();

        @WithName("E09")
        String e09();

        @WithName("E10")
        String e10();

        @WithName("E11")
        String e11();

        @WithName("E12")
        String e12();
    }

    interface Novedad {
        String crear();

        String activar();

        String consultar();
    }

    interface Transaccion {
        String activacion();
    }

    interface Dispositivo {
        String activacion();
    }

    interface Tipo {
        TransaccionTipo transaccion();
    }

    interface TransaccionTipo {
        String activacion();
    }

    interface NumeroBin {
        String consultar();
    }

    interface CodigoRespuesta {
        String ok();

        Error error();
    }

    interface Error {
        String general();

        String estructura();
    }

    interface Descripcion {
        @WithName("E01")
        String e01();

        @WithName("E03")
        String e03();

        @WithName("E08")
        String e08();

        @WithName("E09")
        String e09();

        @WithName("E10")
        String e10();

        @WithName("E11")
        String e11();

        @WithName("E12")
        String e12();
    }
}
