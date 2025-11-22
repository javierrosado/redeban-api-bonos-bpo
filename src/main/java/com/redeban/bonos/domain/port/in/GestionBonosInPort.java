package com.redeban.bonos.domain.port.in;

import com.redeban.bonos.domain.model.ActivarBonoRequest;
import com.redeban.bonos.domain.model.ActivarBonoResponse;
import com.redeban.bonos.domain.model.CrearBonoRequest;
import com.redeban.bonos.domain.model.CrearBonoResponse;
import com.redeban.bonos.domain.model.HeaderContext;
import com.redeban.bonos.domain.model.RecuperarBonoRequest;
import com.redeban.bonos.domain.model.RecuperarBonoResponse;

public interface GestionBonosInPort {

    CrearBonoResponse crearBono(CrearBonoRequest request, HeaderContext headerContext);

    ActivarBonoResponse activarBono(ActivarBonoRequest request, HeaderContext headerContext);

    RecuperarBonoResponse recuperarBono(RecuperarBonoRequest request, HeaderContext headerContext);
}
