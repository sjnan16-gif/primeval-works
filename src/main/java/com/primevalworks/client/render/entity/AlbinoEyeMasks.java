package com.primevalworks.client.render.entity;

final class AlbinoEyeMasks {
    private AlbinoEyeMasks() {
    }

    static boolean contains(String path, int x, int y) {
        if (path.contains("/dodo")) {
            return y >= 33 && y <= 35 && (x >= 2 && x <= 6 || x >= 11 && x <= 15);
        }
        if (path.contains("/t_rex")) {
            return y >= 93 && y <= 94 && (x >= 3 && x <= 9 || x >= 57 && x <= 63);
        }
        if (path.contains("/triceratops")) {
            return y >= 125 && y <= 126 && (x >= 10 && x <= 15 || x >= 26 && x <= 31);
        }
        if (path.contains("/pteranodon")) {
            return y == 89 && (x >= 45 && x <= 56 || x >= 84 && x <= 93);
        }
        if (path.contains("/stegosaurus")) {
            return y >= 52 && y <= 53 && (x >= 106 && x <= 112 || x >= 129 && x <= 135);
        }
        if (path.contains("/parasaurolophus")) {
            return y >= 51 && y <= 53 && (x >= 112 && x <= 116 || x >= 131 && x <= 135);
        }
        if (path.contains("/spino")) {
            return y == 183 && (x >= 191 && x <= 194 || x >= 220 && x <= 223);
        }
        return false;
    }
}
