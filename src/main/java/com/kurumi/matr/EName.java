package com.kurumi.matr;

import java.util.Random;

/**
 * Source of names for towns, streets and rural roads;
 * and numbers for highways.
 * 
 * @author soglesby
 *
 */
class EName {
   static final String factoryTownNames[] = {
      "Springfield", "Shelbyville", "Dunwich", "Innsmouth",
      "Caxily", "Mirocaw", "New Crobuzon", "Tupper Falls",
      "Uncasville", "Temphill", "Rotorua", "Vastarien",
      "Highland", "Gatlin", "Melonville", "Arkham",
      "Carcosa", "Bolton Notch", "Torvalds", "Cicely",
      "Perdido", "Anascovick", "Klendathu", "Dana",
      "Jones Hollow", "Rock Ridge", "Stepford", "Lerxtwood",
      "Black Fork", "Bedford Falls", "Candle Cove", "Kingsport",
      "Sunnydale", "Erfafa", "West Durian", "Jerusalem's Lot",
      "Alanville", "Trinity", "Poroth Farm", "Ambergris",
      "Hickston", "Desperation", "Gilead", "Goatswood", 
      "Brichester", "West Mifflin", "Goblu", "Beatosu"
   };

   static final String factoryStreetNames[] = {
      "Fannin Pl", "Rhodes Rd", "Droz Blvd", "Kirby Blvd",
      "Strudel Ln", "Sriracha St", "Cyburbia St", "Nixon St",
      "Mitchell St", "Agnew Ave", "Dean St", "Liddy St",
      "Sirica St", "North St", "Meese St", "D'Amato St",
      "Woodward Av", "Bernstein Av", "Benefit St", "Aylesbury Pike",
      "Baker St", "Mark Twain Pl", "Updike St", "Salinger St",
      "Fitzgerald Dr", "Roth St", "Oates St", "Mamet St",
      "O'Neill Blvd", "Miller St", "Ling Lane",
      "Cartman St", "Skinner St", "Beavis Way", "Morgendorfer St",
      "Fish St", "Cage St", "Ives St", "Copland St",
      "Partch St", "Ravel St", "Kronos St", "Grainger St",
      "Holst St", "Bartok St", "Orff St", "Plant St",
      "Page St", "Peart St", "Lifeson Av", "Wakeman St",
      "Gabriel St", "Entwistle St", "Daltrey St", "Moon St",
      "Winwood St", "Baker St", "Sensible St", "Scabies St",
      "Flouride St", "East Bay St", "Biafra St", "Peligro St",
      "Stipe St", "Bono St", "Edge St", "Lecter St",
      "Von Helsing St", "Shelley St", "Cassandra St", "Cassilda St",
      "Torvalds St", "Gosling Ave", "Larry Wall Rd", "Wirth Rd", 
      "Phibes St", "Elm St", "Mohegan Rd", "Liebniz Rd", 
      "Newton St", "Pythagoras St", "LaPlace Rd", "Bohr Rd", 
      "Gauss St", "Euler St", "Hilbert St", "Cauchy Rd", 
      "Erdos St", "Fibonacci St", "Huygens St", "Kronecker Rd", 
      "Dirac St", "Ramanujan St", "Shannon St", "Knuth Rd", 
      "Torricelli St", "Fermat St", "Fourier St", "Euclid Rd", 
      "Heisenberg St", "Planck St", "Navier-Stokes Rd", "Riemann Rd", 
      "Brahe St", "Kepler St", "Ptolemy Rd", "Copernicus Rd", 
      "Hawking St", "Mandelbrot St", "Jacoby Rd", "Decartes Rd", 
      "Jung St", "Freud St", "Skinner Rd", "Darwin Rd", 
      "Magritte St", "Duchamp St", "Kandinsky St", "Pollock St", 
      "Kahlo St", "Man Ray St", "Calder St", "Pollock St", 
      "Be-bop Rd", "Fusion St", "Basin St", "Count Basie Rd", 
      "Blofeld St", "Oddjob St", "Nader St", "Dornan St",
      "Gromit St", "Wallace St", "McGwire St", "Sosa St",
      "Ilocano St", "Basque St", "Devanagari St", "Kwakiutl St",
      "Sesuatu Ave", "Brainard St", "Logan Rd", "Dulles St",
      "Narita St", "Rue de Gaulle", "Gatwick Ave", "Heathrow Rd",
      "Liu St", "Posey St", "Mieville St", "Yeoh St",
      "Vandermeer St", "Poe St", "Wandrei Ln", "Howard St",
      "Bloch St", "Campbell St", "Lumley St", "Bradbury St",
      "Belknap St", "Kuttner St", "Ashton St", "Ligotti St",
      "Voudrais St", "Olly St", "Chester Rd", "Jalan Anda Cantik",
      "Queen St"
   };

   static final String factoryRuralRoadNames[] = {
      "Ballard Hwy", "Anderson Pkwy", "County Rd 14", "County Rd 21",
      "Moses Rd", "Old State Rd", "Eisenhower Hwy", "Zzyzx Rd",
      "Hancock Rd", "AA Road", "Bandit Trail", "McNally Rd",
      "Gousha Rd", "Volpe Rd", "Howard-Cramer Hwy", "Clason Tpke",
      "Slater Rd", "Sukarnoputri Hwy"
   };

   // sometimes, you want n/s odd, e/w even
   public static final int noPref = 0;
   public static final int prefOdd = 1;
   public static final int prefEven = 2;

   // instance variables
   private int newNumber = 0;
   
   // when assigning names, we start at a random point in the list,
   // but afterwards proceed in order
   // TODO if we wrap around, we'll get duplicate names
   Random dice = new Random();
   private int nTownsAssigned = 0;
   private int firstTid =  dice.nextInt(Integer.MAX_VALUE) % factoryTownNames.length;
   private int nStreetsAssigned = 0;
   private int firstSid =  dice.nextInt(Integer.MAX_VALUE) % factoryStreetNames.length;
   private int nRuralStreetsAssigned = 0;
   private int firstRuralSid =  dice.nextInt(Integer.MAX_VALUE) % factoryRuralRoadNames.length;

   public EName() {
   }

   // pick a number higher than existing numbers
   // to avoid duplication
   // align oddness with north/southness
   public int pickNewNumber(int prefs) {
      newNumber++;
      if (newNumber > 20) {
         newNumber += dice.nextInt() & 31;
      }
      switch (prefs) {
         case prefOdd:
            if ((newNumber & 1) == 0) {
               newNumber++;
            }
            break;
         case prefEven:
            if ((newNumber & 1) == 1) {
               newNumber++;
            }
            break;
      }
      return newNumber;
   }

   public String pickNewTownName() {
      ++nTownsAssigned;
      int newId = (firstTid + nTownsAssigned) % factoryTownNames.length;
      return factoryTownNames[newId];
   }

   public String pickNewStreetName() {
      ++nStreetsAssigned;
      int newId = (firstSid + nStreetsAssigned) % factoryStreetNames.length;
      return factoryStreetNames[newId];
   }

   public String pickNewRuralStreetName() {
      ++nRuralStreetsAssigned;
      int newId = (firstRuralSid + nRuralStreetsAssigned) %
         factoryRuralRoadNames.length;
      return factoryRuralRoadNames[newId];
   }
}
