package io.github.Elevador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Predio {
    public final int numAndares;
    public final List<List<Passageiro>> filasAndares;
    public final Semaphore mutexPredio = new Semaphore(1, true);

    public Predio(int numAndares) {
        this.numAndares = numAndares;
        filasAndares = new ArrayList<>();
        for (int i = 0; i < numAndares; i++) filasAndares.add(new ArrayList<>());
    }
}