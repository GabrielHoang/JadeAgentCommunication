/*
 *  Klasa agenta kupującego książki
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

// Przykładowa klasa zachowania:
class MyOwnBehaviour extends Behaviour
{
    protected MyOwnBehaviour()
    {
    }
    public void action()
    {
    }
    public boolean done() {
        return false;
    }
}

public class BookBuyerAgent extends Agent {

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private AID[] sellerAgents = {
            new AID("seller1", AID.ISLOCALNAME),
            new AID("seller2", AID.ISLOCALNAME)};

    // Inicjalizacja klasy agenta:
    protected void setup()
    {

        //doWait(3000 + 2019);   // Oczekiwanie na uruchomienie agentów sprzedających

        System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja h 2018/19) jest gotów do kupowania!");

        Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

        if (args != null && args.length > 0)   // jeśli podano tytuł książki
        {
            targetBookTitle = (String) args[0];
            System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

            addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań - kod znajduje się poniżej

        }
        else
        {
            // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
            System.out.println("Należy podać tytuł książki w argumentach wejściowych kupca!");
            doDelete();
        }
    }
    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
        System.out.println("Agent-kupiec "+getAID().getName()+" kończy swoje nędzne istnienie.");
    }

    /**
     Inner class RequestPerformer.
     This is the behaviour used by Book-buyer agents to request seller
     agents the target book.
     */
    private class RequestPerformer extends Behaviour
    {

        private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
        private int bestPrice;      // najlepsza cena
        private int repliesCnt = 0; // liczba odpowiedzi od agentów
        private MessageTemplate mt; // szablon odpowiedzi
        private int step_num = 0;   // numer kroku

        public void action()
        {
            switch (step_num) {
                case 0:      // wysłanie oferty kupna
                    System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
                    for (int i = 0; i < sellerAgents.length; ++i)
                    {
                        System.out.print(sellerAgents[i]+ " ");
                    }
                    System.out.println();

                    // Tworzenie wiadomości CFP do wszystkich sprzedawców:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i)
                    {
                        cfp.addReceiver(sellerAgents[i]);          // dodanie adresate
                    }
                    cfp.setContent(targetBookTitle);             // wpisanie zawartości - tytułu książki
                    cfp.setConversationId("book-trade");         // wpisanie specjalnego identyfikatora korespondencji
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
                    myAgent.send(cfp);                           // wysłanie wiadomości

                    // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step_num = step_num + 1;     // przejście do kolejnego kroku
                    break;
                case 1:      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
                    ACLMessage reply = myAgent.receive(mt);      // odbiór odpowiedzi
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
                        {
                            int price = Integer.parseInt(reply.getContent());  // cena książki
                            if (bestSeller == null || price < bestPrice)       // jeśli jest to najlepsza oferta
                            {
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;                                        // liczba ofert
                        if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
                        {
                            bestPrice = bestPrice/2; //inicjalna propozycja 50% ceny

                            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                            msg.addReceiver(bestSeller);
                            msg.setContent(String.valueOf(bestPrice));
                            msg.setConversationId("book-trade");
                            msg.setReplyWith("price"+System.currentTimeMillis());
                            myAgent.send(msg);
//                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
//                                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                            step_num = step_num + 1;
                            System.out.println("WYBRALEM SPRZEDAWCE " + bestSeller.getName());
                            System.out.println("Agent-kupiec  "+getAID().getName()+" proponuje: " + bestPrice);
                        }
                    }
                    else
                    {
                        block();
                    }
                    break;
                case 2:     //negocjacje
                    ACLMessage sellerPriceReply = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                    ACLMessage sellerPriceReject = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));

                    if (sellerPriceReply != null)
                    {
                            double sellerLastPrice = Double.valueOf(sellerPriceReply.getContent());
                            if((Math.abs(sellerLastPrice-bestPrice) > 4)) {
                                bestPrice +=5; //stałe podwyższenie ceny o 5

                                ACLMessage buyerPriceReply = sellerPriceReply.createReply();
                                buyerPriceReply.setContent(String.valueOf(bestPrice));
                                buyerPriceReply.setPerformative(ACLMessage.PROPOSE);
                                myAgent.send(buyerPriceReply);
                                System.out.println("Agent-kupiec  "+getAID().getName()+" proponuje: " + bestPrice);
                            } else {
                                step_num = 3;
                                System.out.println("------------------------------------------------");
                                System.out.println("Agent-kupiec  "+getAID().getName()+" zgadza się na cenę: " + sellerLastPrice);
                            }

                    } else if (sellerPriceReject !=null) {
                        System.out.println("Agent-kupiec  "+getAID().getName()+" nic dzisiaj nie kupię, idę do domu");
                        step_num = 5;
                    }
                    block();
                    break;
                case 3:      // wysłanie zamówienia do sprzedawcy, który złożył najlepszą ofertę
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step_num = step_num + 1;
                    break;
                case 4:        // odbiór odpowiedzi na zamównienie
                    reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.INFORM)
                        {
                            System.out.println("Tytuł "+targetBookTitle+" został zamówiony.");
                            System.out.println("Cena = "+bestPrice);
                            myAgent.doDelete();
                        }
                        step_num = step_num + 1;
                    }
                    else
                    {
                        block();
                    }
                    break;
            }  // switch
        } // action

        public boolean done() {
            return ((step_num == 3 && bestSeller == null) || step_num == 5);
        }
    } // Koniec wewnętrznej klasy RequestPerformer
}
