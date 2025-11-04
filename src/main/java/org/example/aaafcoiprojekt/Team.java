package org.example.aaafcoiprojekt;

public record Team(
        int id,
        String name,
        String continent,
        int attack,
        int defense,
        String flagFile
) {

    @Override
    public String toString() {
        return name + " (" + continent + ")";
    }
}