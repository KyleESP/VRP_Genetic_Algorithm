package cvrp_population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import operators.CrossoverOperator;
import operators.MutationOperator;
import operators.SelectionOperator;

public class Genetic2 {
	/*
	private CrossoverOperator crossoverOperator;
	private MutationOperator mutationOperator;
	private SelectionOperator selectionOperator;
	private long nbGenerations;
	private int nbIndividuals;
	private double pMutation;
	private int maxCapacity;
	private ArrayList<Location> locations;
	private ArrayList<ArrayList<Vehicle>> population;
    private ArrayList<Vehicle> bestIndividual;
    private double bestCost;
    private ArrayList<Double> costsHistory;
	private Random rand;
	
	public Genetic2(ArrayList<Location> locations, int nbVehicles, int maxCapacity, long nbGenerations, int nbIndividuals, double pMutation) {
    	this.maxCapacity = maxCapacity;
    	this.nbGenerations = nbGenerations;
    	this.nbIndividuals = nbIndividuals;
    	this.pMutation = pMutation;
    	population = new ArrayList<>(nbIndividuals);
    	this.locations = locations;
    	rand = new Random();
    	selectionOperator = new SelectionOperator(this);
    	mutationOperator = new MutationOperator(this);
    	crossoverOperator = new CrossoverOperator(this);
    	costsHistory = new ArrayList<>();
		initPopulation();
	}
	
    public void exec() {
		bestCost = Double.POSITIVE_INFINITY;
		updateBestSolution();
		displayDescription();
		double percentage;
		ArrayList<Vehicle> parent1, parent2, child;
		ArrayList<Vehicle> parentMutation, mutant;
		for (int i = 0; i < nbGenerations; i++) {
			parent1 = selectionOperator.tournament(3);
			parent2 = selectionOperator.tournament(3);
			child = crossoverOperator.hGreXCrossover(parent1, parent2);
			setSimilarIndividual(child);
			if (rand.nextDouble() <= pMutation) {
				parentMutation = getRandomButNotBest();
				mutant = mutationOperator.inversionMutation(parentMutation);
				population.remove(parentMutation);
				population.add(mutant);
			}
			updateBestSolution();
			percentage = ((double)(i + 1) / nbGenerations) * 100;
        	if(percentage % 1 == 0) {
        		System.out.println((int)percentage + "%");
        	}
		}
	    displaySolution(bestIndividual);
    }
	
	private ArrayList<Vehicle> getBestIndividualInCurrentPopulation() {
		double fMin = Double.POSITIVE_INFINITY;
		ArrayList<Vehicle> xMin = null;
		double fCurr;
		for (ArrayList<Vehicle> vehicles : population) {
			if ((fCurr = objectiveFunction(vehicles)) < fMin) {
				fMin = fCurr;
				xMin = vehicles;
			}
		}
		return xMin;
	}
	
	private ArrayList<Vehicle> getRandomButNotBest() {
		ArrayList<Vehicle> bestIndividual = getBestIndividualInCurrentPopulation();
		ArrayList<Vehicle> randomInd;
		do {
			randomInd = population.get(rand.nextInt(population.size()));
		} while(randomInd != bestIndividual);
		return randomInd;
	}
	
	private void setSimilarIndividual(ArrayList<Vehicle> individual) {
		double indCost = objectiveFunction(individual);
		double currIndCost;
		boolean indCostBetter;
		boolean hasSimilar = false;
		for (ArrayList<Vehicle> currInd : population) {
			currIndCost = objectiveFunction(currInd);
			indCostBetter = currIndCost < indCost;
			if (Math.abs(currIndCost - indCost) / (indCostBetter ? currIndCost : indCost) < 0.01) {
				if (indCostBetter) {
					population.remove(currInd);
					population.add(individual);
				}
				hasSimilar = true;
				break;
			}
		}
		if (!hasSimilar) {
			ArrayList<Vehicle> badIndividual = selectionOperator.tournament(2);
			population.remove(badIndividual);
			population.add(individual);
		}
	}
	
	private void initPopulation() {
		for (int i = 0; i < nbIndividuals; i++) {
			ArrayList<Vehicle> vehicles = new ArrayList<>();
			population.add(vehicles);
	        int vIdx = 0;
	        Vehicle v;
	        ArrayList<Location> locationsCopy = Util.createDeepCopyLocations(locations);
	        Location depot = locationsCopy.get(0);
	        Collections.shuffle(locationsCopy, new Random(i));
	        while (hasAnUnroutedLocation(locationsCopy)) {
	        	if (vIdx >= vehicles.size()) {
	        		Vehicle newV = new Vehicle(maxCapacity);
	        		newV.routeLocation(depot);
	        		vehicles.add(vIdx, newV);
	        	}
	        	v = vehicles.get(vIdx);
	            Location choseLocation = null;
	            int currentLocationId = v.getCurrentLocationId();
	            for(Location l : locationsCopy) {
	            	if((currentLocationId != l.getId()) && !l.getIsRouted() && v.fits(l.getNbOrders())) {
	                    choseLocation = l;
	            	}
	            }
	            
	            if(choseLocation != null) {
	            	v.routeLocation(choseLocation);
	            } else {
	                v.routeLocation(depot);
	                vIdx++;
	            }
	        }
	        vehicles.get(vIdx).routeLocation(depot);
		}
    }
    
	private boolean hasAnUnroutedLocation(ArrayList<Location> locations) {
        for(Location l : locations) {
            if (!l.getIsRouted()) {
            	return true;
            }
        }
        return false;
    }
	
	private void updateBestSolution() {
		double fMin = Double.POSITIVE_INFINITY;
		ArrayList<Vehicle> xMin = null;
		double fCurr;
		for (ArrayList<Vehicle> vehicles : population) {
			if ((fCurr = objectiveFunction(vehicles)) < fMin) {
				fMin = fCurr;
				xMin = vehicles;
			}
		}
		if (fMin < bestCost) {
			bestCost = fMin;
			bestIndividual = xMin;
		}
    	costsHistory.add(bestCost);
	}
	
	public <T> ArrayList<Vehicle> reconstruct(ArrayList<T> brokenLocations) {
		ArrayList<Vehicle> newChild = new ArrayList<>();
		Vehicle v = new Vehicle(maxCapacity);
		Location depot = Util.getLocationById(0, locations);
		v.routeLocation(depot);
		Location l;
		for (int i = 0; i < brokenLocations.size(); i++) {
			l = (brokenLocations.get(i) instanceof Integer) ? Util.getLocationById((Integer)brokenLocations.get(i), locations) : (Location)brokenLocations.get(i);
			if (!v.routeLocation(l)) {
				v.routeLocation(depot);
				newChild.add(v);
				v = new Vehicle(maxCapacity);
				v.routeLocation(depot);
				v.routeLocation(l);
			}
		}
		v.routeLocation(depot);
		newChild.add(v);
		return newChild;
	}
	
	public double objectiveFunction(ArrayList<Vehicle> vehicles) {
		double sumDist = 0;
    	double sumVehicles = 0;
    	ArrayList<Location> route;
    	for(Vehicle v : vehicles) {
    		route = v.getRoute();
			sumVehicles++;
			for (int i = 0; i < route.size() - 1; i++) {
    			sumDist += Util.getDistances().get(route.get(i).getId()).get(route.get(i + 1).getId());
    		}
    	}
    	return sumDist + sumVehicles;
    }
	
	public void displayDescription() {
		System.out.println("----------------------------------------------------------------------------------------------------");
		System.out.println("Algorithme g�n�tique :");
		String description = "Co�t initial = " + (double) Math.round(bestCost * 1000) / 1000;
		description += "\nNombre de g�n�rations = " + nbGenerations;
		description += "\nNombre d'individus = " + nbIndividuals;
		description += "\nProbabilit� de mutation = " + pMutation;
		System.out.println(description);
		System.out.println("----------------------------------------------------------------------------------------------------");
	}
	
	public String getInlineDescription() {
		String description = "Co�t final = " + (double) Math.round(bestCost * 1000) / 1000;
		description += " | Nb v�hicules = " + bestIndividual.size() + " | ";
		description += " | Nombre d'individus = " +  nbIndividuals;
		description += " | Nombre de g�n�rations = " + nbGenerations;
		description += " | Probabilit� de mutation = " + pMutation;
		return description;
	}
	
    public void displaySolution(ArrayList<Vehicle> vehicles) {
    	double cost = objectiveFunction(vehicles);
    	System.out.println("----------------------------------------------------------------------------------------------------");
        for (int i = 0 ; i < vehicles.size() ; i++) {
            System.out.println("V�hicule n�" + (i + 1) + " : " + getRouteString(vehicles.get(i).getRoute()));
        }
        System.out.println("\nCo�t de la solution : " + cost);
    	System.out.println("----------------------------------------------------------------------------------------------------");
    }
    
    public String getRouteString(ArrayList<Location> route) {
    	int routeSize = route.size();
    	String routeString = "";
    	for (int i = 0; i < routeSize ; i++) {
        	routeString += "(" + route.get(i).getId() + ")" + ((i != routeSize - 1) ? " == " : "");
        }
    	return routeString;
    }
    
    public ArrayList<ArrayList<Vehicle>> getPopulation() {
    	return population;
    }
    
    public ArrayList<Vehicle> getBestIndividual() {
    	return bestIndividual;
    }
    
    public double getBestCost() {
    	return bestCost;
    }
    
    public ArrayList<Double> getCostsHistory() {
    	return costsHistory;
    }
    
    public Random getRand() {
    	return rand;
    }
    
    public int getMaxCapacity() {
    	return maxCapacity;
    }
    
    public ArrayList<Location> getLocations() {
    	return locations;
    }*/
}
