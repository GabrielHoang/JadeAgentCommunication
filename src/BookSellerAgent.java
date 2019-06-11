/*
 *  Klasa agenta sprzedawcy książek.
 *  Sprzedawca dysponuje katalogiem książek oraz dwoma klasami zachowań:
 *  - OfferRequestsServer - obsługa odpowiedzi na oferty klientów
 *  - PurchaseOrdersServer - obsługa zamówienia klienta
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;

import java.text.DecimalFormat;
import java.util.*;
import java.lang.*;


public class BookSellerAgent extends Agent
{
    // Katalog książek na sprzedaż:
    private Hashtable catalogue;

    private double sellerLastPrice;

    DecimalFormat df = new DecimalFormat("#0.00");


    // Inicjalizacja klasy agenta:
    protected void setup()
    {
        // Tworzenie katalogu książek jako tablicy rozproszonej
        catalogue = new Hashtable();

        Random randomGenerator = new Random();    // generator liczb losowych

        catalogue.put("Zamek", 280+randomGenerator.nextInt(200));        // nazwa książki jako klucz, cena jako wartość
//        catalogue.put("Zamek",80);    //przy tej cenie zawsze kupi
        catalogue.put("Proces", 200+randomGenerator.nextInt(170));
        catalogue.put("Opowiadania", 110+randomGenerator.nextInt(50));
        catalogue.put("Agent 007", 120+randomGenerator.nextInt(70));
        catalogue.put("Poranne meki", 270+randomGenerator.nextInt(80));

        doWait(2000);                     // czekaj 2 sekundy

        System.out.println("Witam! Agent-sprzedawca (wersja h 2018/19) "+getAID().getName()+" jest gotów do sprzedaży");

        addBehaviour(new NegotiationServer());

        // Dodanie zachowania obsługującego odpowiedzi na oferty klientów (kupujących książki):
        addBehaviour(new OfferRequestsServer());

        // Dodanie zachowania obsługującego zamówienie klienta:
        addBehaviour(new PurchaseOrdersServer());
    }

    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
        System.out.println("Agent-sprzedawca (wersja h 2018/19) "+getAID().getName()+" zakończył działalność.");
    }


    /**
     Inner class OfferRequestsServer.
     This is the behaviour used by Book-seller agents to serve incoming requests
     for offer from buyer agents.
     If the requested book is in the local catalogue the seller agent replies
     with a PROPOSE message specifying the price. Otherwise a REFUSE message is
     sent back.
     */
    class OfferRequestsServer extends CyclicBehaviour
    {
        public void action()
        {
            // Tworzenie szablonu wiadomości (wstępne określenie tego, co powinna zawierać wiadomość)
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            // Próba odbioru wiadomości zgodnej z szablonem:
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {  // jeśli nadeszła wiadomość zgodna z ustalonym wcześniej szablonem
                String title = msg.getContent();  // odczytanie tytułu zamawianej książki

                System.out.println("Agent-sprzedawca v.h "+getAID().getName()+" otrzymał wiadomość: "+
                        title);
                ACLMessage reply = msg.createReply();               // tworzenie wiadomości - odpowiedzi
                Integer price = (Integer) catalogue.get(title);     // ustalenie ceny dla podanego tytułu
                sellerLastPrice = price;
                if (price != null) {                                // jeśli taki tytuł jest dostępny
                    reply.setPerformative(ACLMessage.PROPOSE);            // ustalenie typu wiadomości (propozycja)
                    reply.setContent(String.valueOf(price.intValue()));   // umieszczenie ceny w polu zawartości (content)
                    System.out.println("Agent-sprzedawca v.h "+getAID().getName()+" odpowiada: "+
                            price.intValue());
                }
                else {                                              // jeśli tytuł niedostępny
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);         // ustalenie typu wiadomości (odmowa)
                    reply.setContent("tytuł niestety niedostępny");                  // treść wiadomości
                }
                myAgent.send(reply);                                // wysłanie odpowiedzi
            }
            else                       // jeśli wiadomość nie nadeszła, lub była niezgodna z szablonem
            {
                block();                 // blokada metody action() dopóki nie pojawi się nowa wiadomość
            }
        }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour

    class NegotiationServer extends CyclicBehaviour{

        private int negotiationRounds = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                if(negotiationRounds < 4) {
                    double buyerLastPrice = Double.valueOf(msg.getContent()); //odbiór ceny
                    double priceProposition = (sellerLastPrice * 0.75 + buyerLastPrice * 0.25); //modyfikacja ceny
                    sellerLastPrice = priceProposition; //aktualizacja ostatniej ceny sprzedawcy

                    ACLMessage reply = msg.createReply();
                    reply.setContent(String.valueOf(priceProposition));
                    reply.setPerformative(ACLMessage.PROPOSE);

                    System.out.println("------------------------------------------------");
                    System.out.println("RUNDA " + (negotiationRounds+1));
                    System.out.println("Agent-sprzedawca v.h "+getAID().getName()+" odpowiada: "+ priceProposition);
                    negotiationRounds++;

                    myAgent.send(reply);
                } else {
                    ACLMessage rejectMessage = msg.createReply();
                    rejectMessage.setPerformative(ACLMessage.REJECT_PROPOSAL);

                    System.out.println("------------------------------------------------");
                    System.out.println("Agent-sprzedawca v.h "+getAID().getName()+" nie jest zainteresowany sprzedażą");

                    myAgent.send(rejectMessage);
                    done();
                }

            } else {
                block();
            }
        }
    }

    class PurchaseOrdersServer extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null)
            {
                // Message received. Process it
                ACLMessage reply = msg.createReply();
                String title = msg.getContent();
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println("Agent-sprzedawca (wersja h 2018/19) "+getAID().getName()+" sprzedał książkę o tytule: "+title);
                myAgent.send(reply);
            }
        }
    } // Koniec klasy wewnętrznej będącej rozszerzeniem klasy CyclicBehaviour
} // Koniec klasy będącej rozszerzeniem klasy Agent