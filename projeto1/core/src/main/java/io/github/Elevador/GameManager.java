package io.github.Elevador;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class GameManager extends ApplicationAdapter {

    public static final int NUM_ANDARES = 8;
    public static final int CAPACIDADE_ELEVADOR = 3;
    public static final int LARGURA_TELA = 800;
    public static final int ALTURA_TELA = 600;
    public static final int TEMPO_PORTA = 500;
    public static final int ATRASO_MOVIMENTO = 500;

    private Predio predio;
    private Elevador elevador;
    
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.BLACK);
        font.getData().setScale(0.8f); 
        camera = new OrthographicCamera();
        camera.setToOrtho(false, LARGURA_TELA, ALTURA_TELA);

        predio = new Predio(NUM_ANDARES);
        elevador = new Elevador(predio, CAPACIDADE_ELEVADOR);
        
        elevador.start();
        iniciarGeradorPassageiros();
    }

    private void iniciarGeradorPassageiros() {
        Thread gerador = new Thread(() -> {
            Random rand = new Random();
            while (true) {
                try {
                    Thread.sleep(1500 + rand.nextInt(2500));
                    int origem = rand.nextInt(NUM_ANDARES);
                    int destino = rand.nextInt(NUM_ANDARES);
                    while (destino == origem) destino = rand.nextInt(NUM_ANDARES);

                    Passageiro p = new Passageiro(origem, destino, predio, elevador);
                    p.start();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        gerador.setDaemon(true);
        gerador.start();
    }

    @Override
    public void render() {
        
        Gdx.gl.glClearColor(0.56f, 0.93f, 0.56f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        int alturaAndar = ALTURA_TELA / NUM_ANDARES;
        int larguraElevador = 80;
        int xElevador = 100;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        shapeRenderer.setColor(0.18f, 0.31f, 0.31f, 1);
        for (int i = 0; i < NUM_ANDARES; i++) {
            int yChao = i * alturaAndar;
            shapeRenderer.rect(0, yChao, LARGURA_TELA, 10);
        }

        float yElevador = elevador.getAndarAtual() * alturaAndar + 10; 
        shapeRenderer.setColor(0.9f, 0.9f, 0.9f, 1);
        shapeRenderer.rect(xElevador, yElevador, larguraElevador, alturaAndar - 10);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rectLine(xElevador, yElevador, xElevador + larguraElevador, yElevador, 2);
        shapeRenderer.rectLine(xElevador, yElevador + alturaAndar - 10, xElevador + larguraElevador, yElevador + alturaAndar - 10, 2);
        shapeRenderer.rectLine(xElevador, yElevador, xElevador, yElevador + alturaAndar - 10, 2); 
        shapeRenderer.rectLine(xElevador + larguraElevador, yElevador, xElevador + larguraElevador, yElevador + alturaAndar - 10, 2);

        
        if (!elevador.isPortasAbertas()) {
            shapeRenderer.setColor(0.75f, 0.75f, 0.75f, 1);
            shapeRenderer.rect(xElevador + 2, yElevador + 2, larguraElevador - 4, alturaAndar - 14);
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.rectLine(xElevador + larguraElevador/2f, yElevador, xElevador + larguraElevador/2f, yElevador + alturaAndar - 10, 2);
        }

        if (predio.mutexPredio.tryAcquire()) { 
            try {
                for (int i = 0; i < NUM_ANDARES; i++) {
                    List<Passageiro> fila = predio.filasAndares.get(i);
                    int xPessoa = xElevador + 100;
                    int yChao = i * alturaAndar;
                    
                    for (Passageiro p : fila) {
                        desenharPassageiro(p, xPessoa, yChao + 10);
                        xPessoa += 35;
                    }
                }
            } finally {
                predio.mutexPredio.release();
            }
        }
        if (elevador.mutexElevador.tryAcquire()) {
            try {
                List<Passageiro> dentro = elevador.getPassageiros();
                float xBase = xElevador + 5;
                float yBase = yElevador + 5;
                
                for(int k = 0; k < dentro.size(); k++) {
                    Passageiro p = dentro.get(k);
                    float px = xBase + (k % 3) * 25;
                    float py = yBase + (k / 3) * 35;
                    desenharPassageiro(p, px, py);
                }
            } finally {
                elevador.mutexElevador.release();
            }
        }
        shapeRenderer.end();
        batch.begin();

        for (int i = 0; i < NUM_ANDARES; i++) {
            font.draw(batch, "Andar " + i, 10, (i * alturaAndar) + 25);
        }
        
        font.draw(batch, "Cap: " + elevador.getPassageiros().size() + "/" + elevador.getCapacidade(), xElevador, yElevador + alturaAndar + 15);

        if (predio.mutexPredio.tryAcquire()) {
            try {
                for (int i = 0; i < NUM_ANDARES; i++) {
                    List<Passageiro> fila = predio.filasAndares.get(i);
                    int xPessoa = xElevador + 100;
                    int yChao = i * alturaAndar;
                    for (Passageiro p : fila) {
                        font.draw(batch, String.valueOf(p.getDestino()), xPessoa + 7, yChao + 66);
                        xPessoa += 35;
                    }
                }
            } finally {
                predio.mutexPredio.release();
            }
        }

        if (elevador.mutexElevador.tryAcquire()) {
            try {
                List<Passageiro> dentro = elevador.getPassageiros();
                float xBase = xElevador + 5;
                float yBase = yElevador + 5;
                for(int k = 0; k < dentro.size(); k++) {
                    Passageiro p = dentro.get(k);
                    float px = xBase + (k % 3) * 25;
                    float py = yBase + (k / 3) * 35;
                    font.draw(batch, String.valueOf(p.getDestino()), px + 7, py + 56);
                }
            } finally {
                elevador.mutexElevador.release();
            }
        }

        batch.end();
    }

    private void desenharPassageiro(Passageiro p, float x, float y) {
      
        shapeRenderer.setColor(p.corCamisa);
        shapeRenderer.rect(x, y + 10, 20, 15);
        
        shapeRenderer.setColor(p.corPele);
        shapeRenderer.rect(x + 2, y + 25, 16, 16);
      
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(x + 2, y, 6, 10);
        shapeRenderer.rect(x + 12, y, 6, 10);
       
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x + 5, y + 45, 10, 12);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}