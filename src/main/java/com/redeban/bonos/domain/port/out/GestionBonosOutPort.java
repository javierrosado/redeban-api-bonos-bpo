package com.redeban.bonos.domain.port.out;

import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;

public interface GestionBonosOutPort {

    CrearBonoResponse crearBono(CrearBonoRequest request);

    ActivarBonoResponse activarBono(ActivarBonoRequest request);

    RecuperarBonoResponse recuperarBono(RecuperarBonoRequest request);
}
