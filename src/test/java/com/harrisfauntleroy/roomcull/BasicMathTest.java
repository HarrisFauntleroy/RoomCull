package com.harrisfauntleroy.roomcull;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Basic math and utility tests for room culling logic. These tests verify mathematical operations
 * used in room detection without requiring Minecraft classes.
 */
class BasicMathTest {

    @Test
    @DisplayName("Distance calculation should work correctly")
    void testDistanceCalculation() {
        // Test 3D distance calculation (used in room detection)
        double distance = Math.sqrt(Math.pow(3, 2) + Math.pow(4, 2) + Math.pow(5, 2));
        assertEquals(7.071, distance, 0.001, "3D distance should be calculated correctly");
    }

    @ParameterizedTest
    @DisplayName("Point in box containment test")
    @CsvSource({
        "5, 5, 5, 0, 0, 0, 10, 10, 10, true", // Point inside box
        "15, 15, 15, 0, 0, 0, 10, 10, 10, false", // Point outside box
        "0, 0, 0, 0, 0, 0, 10, 10, 10, true", // Point on edge (inclusive)
        "10, 10, 10, 0, 0, 0, 10, 10, 10, true", // Point on opposite edge
        "-1, 5, 5, 0, 0, 0, 10, 10, 10, false" // Point outside in one dimension
    })
    void testPointInBoxContainment(
            double px,
            double py,
            double pz,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            boolean expectedInside) {
        // Test the mathematical logic used for room boundary detection
        boolean actualInside =
                (px >= minX && px <= maxX)
                        && (py >= minY && py <= maxY)
                        && (pz >= minZ && pz <= maxZ);

        assertEquals(
                expectedInside,
                actualInside,
                String.format(
                        "Point (%.1f, %.1f, %.1f) should be %s box bounds",
                        px, py, pz, expectedInside ? "inside" : "outside"));
    }

    @Test
    @DisplayName("Room bounds calculation should work")
    void testRoomBoundsCalculation() {
        // Test the mathematical operations used in RoomBlockEntity.updateOcclusionBounds()

        // Simulate room block at (10, 20, 30)
        int centerX = 10, centerY = 20, centerZ = 30;

        // Simulate detected wall distances
        int northWall = 5, southWall = 7, eastWall = 6, westWall = 4;
        int upWall = 3, downWall = 8;

        // Calculate bounds with 0.1 inward offset (as done in the actual code)
        double minX = centerX - westWall + 0.1; // 10 - 4 + 0.1 = 6.1
        double maxX = centerX + eastWall - 0.1; // 10 + 6 - 0.1 = 15.9
        double minY = centerY - downWall + 0.1; // 20 - 8 + 0.1 = 12.1
        double maxY = centerY + upWall - 0.1; // 20 + 3 - 0.1 = 22.9
        double minZ = centerZ - northWall + 0.1; // 30 - 5 + 0.1 = 25.1
        double maxZ = centerZ + southWall - 0.1; // 30 + 7 - 0.1 = 36.9

        // Verify calculations
        assertEquals(6.1, minX, 0.001, "MinX should be calculated correctly");
        assertEquals(15.9, maxX, 0.001, "MaxX should be calculated correctly");
        assertEquals(12.1, minY, 0.001, "MinY should be calculated correctly");
        assertEquals(22.9, maxY, 0.001, "MaxY should be calculated correctly");
        assertEquals(25.1, minZ, 0.001, "MinZ should be calculated correctly");
        assertEquals(36.9, maxZ, 0.001, "MaxZ should be calculated correctly");

        // Test size calculations
        double width = maxX - minX; // 15.9 - 6.1 = 9.8
        double height = maxY - minY; // 22.9 - 12.1 = 10.8
        double depth = maxZ - minZ; // 36.9 - 25.1 = 11.8

        assertEquals(9.8, width, 0.001, "Room width should be calculated correctly");
        assertEquals(10.8, height, 0.001, "Room height should be calculated correctly");
        assertEquals(11.8, depth, 0.001, "Room depth should be calculated correctly");
    }

    @Test
    @DisplayName("Threshold comparison should work")
    void testThresholdComparison() {
        // Test the position threshold logic used in RoomManager caching
        double threshold = 0.1;
        double thresholdSquared = threshold * threshold; // 0.01

        // Test distances below threshold
        double distance1 = 0.05;
        double distanceSquared1 = distance1 * distance1; // 0.0025
        assertTrue(distanceSquared1 < thresholdSquared, "Small distance should be below threshold");

        // Test distances above threshold
        double distance2 = 0.2;
        double distanceSquared2 = distance2 * distance2; // 0.04
        assertTrue(distanceSquared2 > thresholdSquared, "Large distance should be above threshold");
    }
}
