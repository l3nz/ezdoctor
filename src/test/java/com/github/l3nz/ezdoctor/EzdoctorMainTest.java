package com.github.l3nz.ezdoctor;

import org.junit.Test;

import static org.junit.Assert.*;

public class EzdoctorMainTest {

    @Test
    public void versionConstantIsDefined() {
        assertNotNull(EzdoctorMain.VERSION);
        assertFalse(EzdoctorMain.VERSION.isEmpty());
    }
}
