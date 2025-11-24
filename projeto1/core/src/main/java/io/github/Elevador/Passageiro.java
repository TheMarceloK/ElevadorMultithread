package io.github.Elevador;

import java.util.Random;
import java.util.concurrent.Semaphore;

import com.badlogic.gdx.graphics.Color;

class Passageiro extends Thread {
    private final int origem, destino;
    private final Predio predio;
    private final Elevador elevador;
    
    public final Color corCamisa;
    public final Color corPele;
    
    public final Semaphore semSinalElevador = new Semaphore(0, true);

    public Passageiro(int origem, int destino, Predio predio, Elevador elevador) {
        this.origem = origem;
        this.destino = destino;
        this.predio = predio;
        this.elevador = elevador;
        
        Random r = new Random();
        this.corCamisa = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1f);
        
        float[][] peles = {
            {1f, 0.86f, 0.69f}, {0.9f, 0.76f, 0.6f}, {0.55f, 0.33f, 0.14f}, {0.24f, 0.18f, 0.16f}
        };
        float[] escolhida = peles[r.nextInt(peles.length)];
        this.corPele = new Color(escolhida[0], escolhida[1], escolhida[2], 1f);
    }

    public int getDestino() { return destino; }

    @Override
    public void run() {
        try {
            predio.mutexPredio.acquire();
            predio.filasAndares.get(origem).add(this);
            predio.mutexPredio.release();

            elevador.semEsperaRequisicao.release();

            semSinalElevador.acquire();
            Thread.sleep(300); 
            elevador.registrarEntrada(this);
            elevador.semConfirmacaoMovimento.release();

            semSinalElevador.acquire();
            Thread.sleep(300); 
            elevador.registrarSaida(this);
            elevador.semConfirmacaoMovimento.release();

        } catch (InterruptedException e) { e.printStackTrace(); }
    }
}