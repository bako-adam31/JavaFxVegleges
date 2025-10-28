package org.example.aaafcoiprojekt;

public record Team(
        int id,
        String name,
        String continent,
        int attack,
        int defense
) {

    @Override
    public String toString() {
        return name + " (" + continent + ")";
    }
}