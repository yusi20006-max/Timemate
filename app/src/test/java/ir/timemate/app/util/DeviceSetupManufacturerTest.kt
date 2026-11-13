package ir.timemate.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSetupManufacturerTest {
    @Test
    fun detectsXiaomiRedmiAndPocoBrands() {
        assertEquals(DeviceSetupManufacturer.XIAOMI, DeviceSetupManufacturer.detect("Xiaomi", "Xiaomi"))
        assertEquals(DeviceSetupManufacturer.XIAOMI, DeviceSetupManufacturer.detect("Redmi", "Redmi"))
        assertEquals(DeviceSetupManufacturer.XIAOMI, DeviceSetupManufacturer.detect("POCO", "POCO"))
    }

    @Test
    fun detectsSamsungAndFallsBackToGenericAndroid() {
        assertEquals(DeviceSetupManufacturer.SAMSUNG, DeviceSetupManufacturer.detect("samsung", "SM-S928B"))
        assertEquals(DeviceSetupManufacturer.OTHER, DeviceSetupManufacturer.detect("Google", "Pixel"))
        assertEquals(DeviceSetupManufacturer.OTHER, DeviceSetupManufacturer.detect("motorola", "motorola"))
    }

    @Test
    fun storedManufacturerIdsAreStable() {
        assertEquals(DeviceSetupManufacturer.XIAOMI, DeviceSetupManufacturer.fromStorage("xiaomi"))
        assertEquals(DeviceSetupManufacturer.SAMSUNG, DeviceSetupManufacturer.fromStorage("samsung"))
        assertEquals(DeviceSetupManufacturer.OTHER, DeviceSetupManufacturer.fromStorage("other"))
        assertEquals(null, DeviceSetupManufacturer.fromStorage("unknown"))
    }
}
