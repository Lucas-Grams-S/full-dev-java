package com.payflow.fixtures.modbad.reports;

import com.payflow.fixtures.modbad.core.api.CoreFacade;
import com.payflow.fixtures.modbad.extras.ExtraService;

public class ReportService {
    private CoreFacade core;        // permitido: core::api
    private ExtraService extras;    // NÃO permitido pelo allowedDependencies
}
