package com.beautyshop.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryRulesTest {
    @Test void availableSubtractsReserved() {
        assertEquals(8, InventoryRules.available(10, 2));
    }
    @Test void reservedCannotExceedOnHand() {
        assertThrows(IllegalArgumentException.class, () -> InventoryRules.available(1, 2));
    }
}
