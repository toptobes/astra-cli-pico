package com.dtsx.astra.cli.core.exceptions.internal.role;

import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.models.RoleRef;
import com.dtsx.astra.cli.core.output.AstraColors;

public class RoleNotFoundException extends AstraCliException {
    public RoleNotFoundException(RoleRef role) {
        super("""
          @|bold,red Error: Role '%s' not found.|@
        
          The specified role does not exist.
        
          Use %s to see all available roles.
        """.formatted(
            role,
            AstraColors.highlight("astra role list")
        ));
    }
}
