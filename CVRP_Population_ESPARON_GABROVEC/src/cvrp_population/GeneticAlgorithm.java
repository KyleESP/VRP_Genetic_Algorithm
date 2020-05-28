package cvrp_population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;

import operators.CrossoverOperator;
import operators.MutationOperator;
import operators.SelectionOperator;

public class GeneticAlgorithm {
	
	private CrossoverOperator crossoverOperator;
	private MutationOperator mutationOperator;
	private SelectionOperator selectionOperator;
	private long nbGenerations;
	private int nbIndividuals;
	private double pMutation;
	private int maxCapacity;
	private double diffRate;
	private ArrayList<Location> locations;
	private ArrayList<ArrayList<Vehicle>> population;
    private ArrayList<Vehicle> bestIndividual;
    private double bestCost;
    private TreeMap<Integer, Double> bestCostsHistory;
	private Random rand;
	
	public GeneticAlgorithm(ArrayList<Location> locations, int maxCapacity, long nbGenerations, int nbIndividuals, double pMutation, double diffRate) {
    	this.maxCapacity = maxCapacity;
    	this.nbGenerations = nbGenerations;
    	this.nbIndividuals = nbIndividuals;
    	this.pMutation = pMutation;
    	this.diffRate = diffRate;
    	population = new ArrayList<>(nbIndividuals);
    	this.locations = locations;
    	rand = new Random();
    	selectionOperator = new SelectionOperator(this);
    	mutationOperator = new MutationOperator(this);
    	crossoverOperator = new CrossoverOperator(this);
    	bestCostsHistory = new TreeMap<>();
	}
	
    public void exec() {
    	initPopulation();
		displayDescription();
		int percentage = -1, newPercentage;
		ArrayList<Vehicle> parent1, parent2, child;
		ArrayList<Vehicle> parentMutation, mutant;
		for (int i = 1; i <= nbGenerations; i++) {
			parent1 = selectionOperator.tournament(3);
			parent2 = selectionOperator.tournament(3);
			child = crossoverOperator.hGreXCrossover(parent1, parent2);
			child = descent(child);
			setSimilarIndividual(child, i);
			if (rand.nextDouble() < pMutation) {
				parentMutation = getRandomButNotBest();
				mutant = rand.nextDouble() < 0.5 ? mutationOperator.inversionMutation(parentMutation) 
						: mutationOperator.displacementMutation(parentMutation);
				mutant = descent(mutant);
				population.remove(parentMutation);
				population.add(mutant);
				updateBestIndividual(mutant, objectiveFunction(mutant), i);
			}
			if ((newPercentage = (int)(((double)(i + 1) / nbGenerations) * 100)) != percentage) {
				percentage = newPercentage;
				System.out.println(percentage + "%");
			}
		}
	    displayIndividual(bestIndividual);
    }
	
	private ArrayList<Vehicle> getRandomButNotBest() {
		ArrayList<ArrayList<Vehicle>> notBest = new ArrayList<>();
		for (ArrayList<Vehicle> individual : population) {
			if (objectiveFunction(individual) != bestCost) {
				notBest.add(individual);
			}
		}
		return !notBest.isEmpty() ? notBest.get(rand.nextInt(notBest.size())) : population.get(0);
	}
	
	private void setSimilarIndividual(ArrayList<Vehicle> individual, int i) {
		double indCost = objectiveFunction(individual), currIndCost;
		ArrayList<Vehicle> worst;
		boolean hasSimilar = false;
		if (areSimilar(bestCost, indCost, diffRate)) {
			for (ArrayList<Vehicle> currInd : population) {
				if (areSimilar(bestCost, (currIndCost = objectiveFunction(currInd)), diffRate)) {
					hasSimilar = true;
					if (indCost < currIndCost) {
						population.remove(currInd);
						population.add(individual);
						updateBestIndividual(individual, indCost, i);
						break;
					}
				}
			}
		}
		if (!hasSimilar && (worst = getWorst(indCost)) != null) {
			population.remove(worst);
			population.add(individual);
			updateBestIndividual(individual, indCost, i);
		}
	}
	
	private ArrayList<Vehicle> getWorst(double cost) {
		ArrayList<ArrayList<Vehicle>> worst = new ArrayList<>();
		for (ArrayList<Vehicle> individual : population) {
			if (cost < objectiveFunction(individual)) {
				worst.add(individual);
			}
		}
		return !worst.isEmpty() ? worst.get(rand.nextInt(worst.size())) : null;
	}
	
	private boolean areSimilar(double a, double b, double perc) {
		return Math.abs(a - b) / Math.min(a, b) <= perc;
	}
	
	private void initPopulation() {
		double minCost = Double.POSITIVE_INFINITY, currCost;
		ArrayList<Vehicle> minInd = null;
		ArrayList<Location> locationsCopy = Util.createDeepCopyLocations(locations);
		locationsCopy.remove(Util.getLocationById(0, locationsCopy));
		for (int i = 0; i < nbIndividuals; i++) {
	        Collections.shuffle(locationsCopy);
	        ArrayList<Vehicle> individual = reconstruct(locationsCopy);
	        population.add(individual);
	        if ((currCost = objectiveFunction(individual)) < minCost) {
	        	minCost = currCost;
	        	minInd = individual;
	        }
		}
		bestCost = minCost;
		bestIndividual = minInd;
		bestCostsHistory.put(0, bestCost);
    }
	
	private void updateBestIndividual(ArrayList<Vehicle> individual, double cost, int i) {
		if (cost < bestCost) {
			bestCost = cost;
			bestIndividual = individual;
			bestCostsHistory.put(i, bestCost);
		}
	}
	
	public <T> ArrayList<Vehicle> reconstruct(ArrayList<T> reconstructibleLocations) {
		ArrayList<Vehicle> reconstructedLocations = new ArrayList<>();
		Location depot = Util.getLocationById(0, locations), l;
		Vehicle v = new Vehicle(maxCapacity);
		v.routeLocation(depot);
		for (int i = 0; i < reconstructibleLocations.size(); i++) {
			l = (reconstructibleLocations.get(i) instanceof Integer) ? Util.getLocationById((Integer)reconstructibleLocations.get(i), locations) : (Location)reconstructibleLocations.get(i);
			if (!v.routeLocation(l)) {
				v.routeLocation(depot);
				reconstructedLocations.add(v);
				v = new Vehicle(maxCapacity);
				v.routeLocation(depot);
				v.routeLocation(l);
			}
		}
		v.routeLocation(depot);
		reconstructedLocations.add(v);
		return reconstructedLocations;
	}
	
	public double objectiveFunction(ArrayList<Vehicle> vehicles) {
		double sumDist = 0, sumVehicles = 0;
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
	
    public void displayIndividual(ArrayList<Vehicle> vehicles) {
    	System.out.println("----------------------------------------------------------------------------------------------------");
        for (int i = 0 ; i < vehicles.size() ; i++) {
            System.out.println("V�hicule n�" + (i + 1) + " : " + getRouteString(vehicles.get(i).getRoute()));
        }
        System.out.println("\nCo�t de la solution : " + objectiveFunction(vehicles));
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
    
    public TreeMap<Integer, Double> getBestCostsHistory() {
    	return bestCostsHistory;
    }
    
    public Random getRand() {
    	return rand;
    }
    
    public int getMaxCapacity() {
    	return maxCapacity;
    }
    
    public ArrayList<Location> getLocations() {
    	return locations;
    }
    
    public int getNbIndividuals() {
    	return nbIndividuals;
    }
    
    private ArrayList<Vehicle> descent(ArrayList<Vehicle> individual) {
    	ArrayList<Vehicle> neighbor;
    	while ((neighbor = getBetterNeighbor(individual)) != null) {
    		individual = neighbor;
    	}
    	return individual;
    }
    
    private ArrayList<Vehicle> getBetterNeighbor(ArrayList<Vehicle> individual) {
    	double minCost = objectiveFunction(individual), currCost;
    	ArrayList<Vehicle> bestInd = null, newInd;
		ArrayList<Location> routeFrom, routeTo;
		int routeFromSize, routeToSize;
		boolean isSameRoute;
		for (int vFromIdx = 0; vFromIdx < individual.size(); vFromIdx++) {
			routeFrom = individual.get(vFromIdx).getRoute();
			routeFromSize = routeFrom.size();
			for (int vToIdx = vFromIdx; vToIdx < individual.size(); vToIdx++) {
				routeTo = individual.get(vToIdx).getRoute();
				routeToSize = routeTo.size();
				if (!(isSameRoute = (vFromIdx == vToIdx)) 
						&& individual.get(vFromIdx).getCurrentLoading() + individual.get(vToIdx).getCurrentLoading() > (maxCapacity * 2)) {
					continue;
				}
				for (int locFromIdx = 1; locFromIdx < routeFromSize - (isSameRoute ? 2 : 1); locFromIdx++) {
					for (int locToIdx = (isSameRoute ? (locFromIdx + 1) : (locFromIdx == routeFromSize - 2 ? 1 : 0)); locToIdx < routeToSize - ((isSameRoute && locFromIdx == 1 || !isSameRoute && locFromIdx == routeFromSize - 2) ? 2 : 1); locToIdx++) {
						if ((newInd = isSameRoute ? swapTwoOpt(individual, vFromIdx, locFromIdx, locToIdx) 
								: swapRoutes(individual, routeFrom, routeTo, vFromIdx, vToIdx, locFromIdx, locToIdx)) != null 
								&& (currCost = objectiveFunction(newInd)) < minCost) {
							minCost = currCost;
							bestInd = newInd;
						}
					}
				}
			}
		}
		return bestInd;
	}
    
	private ArrayList<Vehicle> swapRoutes(ArrayList<Vehicle> individual, ArrayList<Location> routeFrom, ArrayList<Location> routeTo, int vFromIdx, int vToIdx, int locFromIdx, int locToIdx) {
		Vehicle newVFrom = new Vehicle(maxCapacity);
		Vehicle newVTo = new Vehicle(maxCapacity);
		int i;
        for (i = 0; i <= locFromIdx; i++) {
        	newVFrom.routeLocation(routeFrom.get(i));
        }
        for (i = locToIdx + 1; i < routeTo.size(); i++) {
        	if (!newVFrom.routeLocation(routeTo.get(i))) {
        		return null;
        	}
        }
        
        for (i = 0; i <= locToIdx; i++) {
        	newVTo.routeLocation(routeTo.get(i));
        }
        for (i = locFromIdx + 1; i < routeFrom.size(); i++) {
        	if (!newVTo.routeLocation(routeFrom.get(i))) {
        		return null;
        	}
        }
        ArrayList<Vehicle> newVehicles = Util.createDeepCopyVehicles(individual);
        newVehicles.set(vFromIdx, newVFrom);
        newVehicles.set(vToIdx, newVTo);
		return newVehicles;
    }
	
	private ArrayList<Vehicle> swapTwoOpt(ArrayList<Vehicle> individual, int vIdx, int locFromIdx, int locToIdx) {
		Vehicle newV = new Vehicle(maxCapacity);
		ArrayList<Location> route = individual.get(vIdx).getRoute();
		
		int i;
        for (i = 0; i <= locFromIdx - 1; i++) {
            newV.routeLocation(route.get(i));
        }
        int dcr = 0;
        for (i = locFromIdx; i <= locToIdx; i++) {
            newV.routeLocation(route.get(locToIdx - dcr));
            dcr++;
        }
        for (i = locToIdx + 1; i < route.size(); i++) {
        	newV.routeLocation(route.get(i));
        }
        ArrayList<Vehicle> newVehicles = Util.createDeepCopyVehicles(individual);
        newVehicles.set(vIdx, newV);
        return newVehicles;
    }
}
