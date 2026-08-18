package fr.project.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PlanningSolverApplication — le point d'entrée du moteur.
 *
 * <h3>Pourquoi il a déménagé</h3>
 * <p>Il vivait jusqu'au 2026-08-18 dans {@code com.example.planning}, vestige du POC initial, et
 * devait déclarer {@code scanBasePackages = "fr.project.planning"} pour aller chercher le moteur
 * dans un autre package que le sien. Cette déclaration était le seul lien qui tenait encore le
 * vestige debout : le reste de {@code com.example} — un domaine, un solveur et un contrôleur de
 * l'époque du POC — n'était ni scanné par Spring, ni compris dans la suite de tests.</p>
 *
 * <p>Une racine que Spring ne scanne pas, dans un package que rien n'utilise, est exactement le
 * genre de détail qu'un nouvel arrivant prend pour la carte du projet. Le point d'entrée est
 * revenu à la racine du moteur, et {@code com.example} a été retiré.</p>
 *
 * <h3>Ce que cette classe ne fait pas</h3>
 * <p>Aucun {@code scanBasePackages} : le scan par défaut part du package de cette classe, qui est
 * désormais celui du moteur. Aucune configuration non plus — elle vit dans
 * {@code application.properties} et dans les classes {@code @Configuration} du moteur.</p>
 */
@SpringBootApplication
public class PlanningSolverApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanningSolverApplication.class, args);
    }
}
