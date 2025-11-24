package io.github.Elevador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Elevador extends Thread {
    private int andarAtual = 0;
    private boolean portasAbertas = false;
    private boolean subindo = true;
    private final int capacidade;
    private final Predio predio;
    private final List<Passageiro> passageirosABordo = new ArrayList<>();

    public final Semaphore mutexElevador = new Semaphore(1, true);
    public final Semaphore semEsperaRequisicao = new Semaphore(0, true);
    public final Semaphore semConfirmacaoMovimento = new Semaphore(0, true);

    public Elevador(Predio predio, int capacidade) {
        this.predio = predio;
        this.capacidade = capacidade;
    }

    public int getAndarAtual() { return andarAtual; }
    public boolean isPortasAbertas() { return portasAbertas; }
    public List<Passageiro> getPassageiros() { return passageirosABordo; }
    public int getCapacidade() { return capacidade; }

    @Override
    public void run() {
        while (true) {
            try {
                boolean temTrabalho = verificarTrabalho();
                if (!temTrabalho) semEsperaRequisicao.acquire();
                
                processarAndarAtual();
                visitarAndar();
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private boolean verificarTrabalho() throws InterruptedException {
        predio.mutexPredio.acquire();
        mutexElevador.acquire();
        boolean genteNoPredio = false;
        for (List<Passageiro> fila : predio.filasAndares) {
            if (!fila.isEmpty()) { genteNoPredio = true; break; }
        }
        boolean genteNoElevador = !passageirosABordo.isEmpty();
        mutexElevador.release();
        predio.mutexPredio.release();
        return genteNoPredio || genteNoElevador;
    }

    private void processarAndarAtual() throws InterruptedException {
        boolean sair = false, entrar = false;
        
        mutexElevador.acquire();
        for (Passageiro p : passageirosABordo) if (p.getDestino() == andarAtual) sair = true;
        mutexElevador.release();

        predio.mutexPredio.acquire();
        if (!predio.filasAndares.get(andarAtual).isEmpty()) entrar = true;
        predio.mutexPredio.release();

        if (!sair && (!entrar || isFull())) return;

        abrirPorta();

        mutexElevador.acquire();
        List<Passageiro> copia = new ArrayList<>(passageirosABordo);
        mutexElevador.release();
        for (Passageiro p : copia) {
            if (p.getDestino() == andarAtual) {
                p.semSinalElevador.release();
                semConfirmacaoMovimento.acquire();
            }
        }

        while (true) {
            if (isFull()) break;
            Passageiro prox = null;
            predio.mutexPredio.acquire();
            if (!predio.filasAndares.get(andarAtual).isEmpty()) prox = predio.filasAndares.get(andarAtual).get(0);
            predio.mutexPredio.release();

            if (prox == null) break;
            prox.semSinalElevador.release();
            semConfirmacaoMovimento.acquire();
        }
        fecharPorta();
    }

    private void abrirPorta() throws InterruptedException {
        portasAbertas = true;
        Thread.sleep(GameManager.TEMPO_PORTA);
    }
    private void fecharPorta() throws InterruptedException {
        Thread.sleep(GameManager.TEMPO_PORTA);
        portasAbertas = false;
    }
    private boolean isFull() throws InterruptedException {
        mutexElevador.acquire();
        boolean f = passageirosABordo.size() >= capacidade;
        mutexElevador.release();
        return f;
    }

    public void registrarEntrada(Passageiro p) throws InterruptedException {
        mutexElevador.acquire();
        passageirosABordo.add(p);
        mutexElevador.release();
        predio.mutexPredio.acquire();
        predio.filasAndares.get(andarAtual).remove(p);
        predio.mutexPredio.release();
    }

    public void registrarSaida(Passageiro p) throws InterruptedException {
        mutexElevador.acquire();
        passageirosABordo.remove(p);
        mutexElevador.release();
    }

    private void visitarAndar() throws InterruptedException {
        predio.mutexPredio.acquire();
        mutexElevador.acquire();

        if (andarAtual == 0) subindo = true;
        if (andarAtual == predio.numAndares - 1) subindo = false;

        boolean temPedidoNaDirecao = false;
        for (Passageiro p : passageirosABordo) {
            if (subindo && p.getDestino() > andarAtual) temPedidoNaDirecao = true;
            if (!subindo && p.getDestino() < andarAtual) temPedidoNaDirecao = true;
        }
        if (!temPedidoNaDirecao) {
            int start = subindo ? andarAtual + 1 : andarAtual - 1;
            int end = subindo ? predio.numAndares : -1;
            int step = subindo ? 1 : -1;
            for (int i = start; i != end; i += step) {
                if (!predio.filasAndares.get(i).isEmpty()) {
                    temPedidoNaDirecao = true;
                    break;
                }
            }
        }
        if (!temPedidoNaDirecao) subindo = !subindo;

        mutexElevador.release();
        predio.mutexPredio.release();

        Thread.sleep(GameManager.ATRASO_MOVIMENTO);
        if (subindo && andarAtual < predio.numAndares - 1) andarAtual++;
        else if (!subindo && andarAtual > 0) andarAtual--;
    }
}