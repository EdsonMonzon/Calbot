package nuevos;

import robocode.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * Clase que representa a un robot de combate en el juego Robocode.
 * @author Edson, Erick, Iker y Mark
 */
public class Calbot extends Robot {

    final double DISTANCIA_MINIMA_ACERCAMIENTO = 20;
    final double ANGULO_INCREMENTO_GIRO_PANICO = 90;
    final double ANGULO_INCREMENTO_ACIERTE = 30;


    double distancia;
    boolean enPosicionInicial = false;
    int comportamientoEnemigo = 5;
    ArrayList<Double> posicionesEnemigas = new ArrayList<>();
    boolean enModoPanico = false;
    Random random = new Random();
    boolean acribillando = false;
    double posicionXInicial = getBattleFieldWidth() / 2 - 180;
    double posicionYInicial = getBattleFieldHeight() / 2 - 90;

    /**
     * Método principal que define las accion del robot al empezar la ronda
     */
    public void run() {
        if (!acribillando) {
            moveTo(posicionXInicial, posicionYInicial);
            lookAt(0);
            enPosicionInicial = true;
            track();
            defineComportamiento();
        }

        while (true) {
            if (!enModoPanico) {
                // Formar el polígono de 8 lados
                if (comportamientoEnemigo == 1) {
                    ahead(150);
                    turnRight(40);
                    turnGunLeft(50);
                } else if (comportamientoEnemigo == 2) {
                    back(150);
                    turnLeft(40);
                    turnGunRight(50);
                } else if (comportamientoEnemigo == 3) {
                    ahead(200);
                    turnRight(45);
                    turnGunLeft(360);
                }
            } else {
                if (!acribillando) {
                    turnRight(random.nextDouble() * ANGULO_INCREMENTO_GIRO_PANICO);
                    ahead(200);
                }
            }

            if (!acribillando) {
                if (getEnergy() % 25 == 0 && enPosicionInicial) {
                    track();
                    defineComportamiento();
                }
            }
        }
    }

    /**
     * Detecta al robot enemigo desde la misma posicion 2 veces
     */
    public void track() {
        turnGunRight(360);
        back(100);
        ahead(100);
        turnGunRight(360);
    }

    /**
     * Apunta el robot hacia un ángulo específico.
     *
     * @param angle El ángulo al que se debe apuntar.
     */
    public void lookAt(double angle) {
        turnRight(normalizeBearing(angle - getHeading()));
    }

    /**
     * Mueve el robot hacia una posición específica.
     *
     * @param x La coordenada X de la posición a la que moverse.
     * @param y La coordenada Y de la posición a la que moverse.
     */
    public void moveTo(double x, double y) {
        double dx = x - getX();
        double dy = y - getY();
        double angleToCenter = Math.toDegrees(Math.atan2(dx, dy));

        lookAt(angleToCenter);
        ahead(Math.hypot(dx, dy));
    }

    /**
     * Normaliza el ángulo para que esté en el rango [-180, 180].
     *
     * @param angle El ángulo a normalizar.
     * @return El ángulo normalizado.
     */
    private double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Maneja el evento cuando el robot golpea a otro robot.
     *
     * @param e El evento de golpear un robot.
     */
    public void onHitRobot(HitRobotEvent e) {
        if (enModoPanico) {
            comportamientoEnemigo = 3;
            turnRadarRight(360);
        }
    }

    /**
     * El robot se mantiene disparando en una posicion hasta que el enemigo se aleje
     *
     * @param e El evento de escanear un robot.
     */
    public void acribillar(ScannedRobotEvent e) {
        while (distancia < DISTANCIA_MINIMA_ACERCAMIENTO) {
            fire(3);
        }
        acribillando = false;
    }

    /**
     * Maneja el evento cuando el robot escanea a otro robot.
     *
     * @param e El evento de escanear un robot.
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        distancia = e.getDistance();

        if (distancia < DISTANCIA_MINIMA_ACERCAMIENTO) {
            acribillar(e);
        }

        if (!enModoPanico) {
            if (comportamientoEnemigo == 3) {
                turnGunRight(perfectGun(e));
            } else if (comportamientoEnemigo == 2) {
                turnGunLeft(perfectGun(e) + ANGULO_INCREMENTO_ACIERTE);
            } else if (comportamientoEnemigo == 1) {
                turnGunRight(perfectGun(e) + ANGULO_INCREMENTO_ACIERTE);
            }

            if (distancia < 100) {
                fire(3); // Mayor potencia para corta distancia
            } else if (distancia < 300) {
                fire(2);
            } else if (distancia < 600) {
                fire(1);
            }

            if (enPosicionInicial) {
                posicionesEnemigas.add(getHeading() + e.getBearing());

                if (getEnergy() < 25) {
                    modoPanico();
                }
            }
        }
    }

    /**
     * Define el comportamiento del robot en función del movimiento enemigo.
     */
    public void defineComportamiento() {
        double primeraPos = 0;
        double segundaPos = 0;

        // Iterar sobre las últimas dos posiciones de los enemigos
        for (int i = posicionesEnemigas.size() - 1; i >= posicionesEnemigas.size() - 2; i--) {
            if (i == posicionesEnemigas.size() - 1) {
                segundaPos = posicionesEnemigas.get(i);
            } else if (i == posicionesEnemigas.size() - 2) {
                primeraPos = posicionesEnemigas.get(i);
            }
        }

        // Determinar el comportamiento basado en las posiciones
        if (segundaPos > primeraPos) {
            comportamientoEnemigo = 1;
        } else if (segundaPos < primeraPos) {
            comportamientoEnemigo = 2;
        } else if (primeraPos == segundaPos) {
            comportamientoEnemigo = 3;
        }
    }

    /**
     * Activa el modo pánico.
     */
    public void modoPanico() {
        if (!acribillando) {
            enModoPanico = true;
        }
    }

    /**
     * Maneja el evento cuando el robot golpea una pared.
     *
     * @param e El evento de golpear una pared.
     */
    public void onHitWall(HitWallEvent e) {
        if (!acribillando) {
            moveTo(posicionXInicial, posicionYInicial);
        }
    }

    /**
     * Calcula el ajuste para el ángulo del cañón para apuntar al enemigo.
     *
     * @param e El evento de escanear un robot.
     * @return El ajuste del ángulo del cañón.
     */
    public double perfectGun(ScannedRobotEvent e) {
        double ajuste = normalizeBearing(e.getBearing() + getHeading() - getGunHeading());
        return ajuste;
    }
}

