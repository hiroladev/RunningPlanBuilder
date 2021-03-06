package de.hirola.runningplanbuilder.model;

import de.hirola.sportsapplications.model.MovementType;
import de.hirola.sportsapplications.model.RunningUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Copyright 2022 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A helper class to view running units in a table view.
 *
 * @author Michael Schmidt (Hirola)
 * @since v0.1
 */
public class RunningUnitTableObject {
    private final String duration;
    private final String key;
    private final String name;
    private final String runningInfos; // for iCAL

    public RunningUnitTableObject(@NotNull RunningUnit runningUnit) {
        duration = runningUnit.getDuration() + " min";
        runningInfos = runningUnit.getRunningInfos();
        MovementType movementType = runningUnit.getMovementType();
        key = movementType.getKey();
        name = movementType.getName();
    }

    public String getDuration() {
        return duration;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getRunningInfos() {
        return runningInfos;
    }
}
