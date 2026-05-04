package com.github.l3nz.ezdoctor;

import org.junit.Test;

import static org.junit.Assert.*;

public class EzdoctorMainTest {

    @Test
    public void versionIsDefined() {
        assertNotNull(Version.get().version);
        assertFalse(Version.get().version.isEmpty());
    }
}
