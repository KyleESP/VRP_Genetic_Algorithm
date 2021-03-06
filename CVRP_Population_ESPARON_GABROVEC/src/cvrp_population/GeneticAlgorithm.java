package cvrp_population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;

import operators.CrossoverOperator;
import operators.MutationOperator;
import operators.SelectionOperator;
import operators.TransformationOperator;

public class GeneticAlgorithm {

	private CrossoverOperator crossoverOperator;
	private MutationOperator mutationOperator;
	private SelectionOperator selectionOperator;
	private TransformationOperator transformationOperator;
	private long nbGenerations;
	private int nbIndividuals;
	private double pMutation;
	private int maxCapacity;
	private double diffRate;
	private boolean isTournament;
	private boolean isHGreX;
	private boolean isHybrid;
	private ArrayList<Location> locations;
	private ArrayList<ArrayList<Vehicle>> population;
    private ArrayList<Vehicle> bestIndividual;
    private double bestCost;
    private TreeMap<Integer, Double> bestCostsHistory;
	private Random rand;
	
	public GeneticAlgorithm(ArrayList<Location> locations, int maxCapacity, long nbGenerations, int nbIndividuals, 
			double pMutation, double diffRate, boolean isTournament, boolean isHGreX, boolean isHybrid) {
    	this.maxCapacity = maxCapacity;
    	this.nbGenerations = nbGenerations;
    	this.nbIndividuals = nbIndividuals;
    	this.pMutation = pMutation;
    	this.diffRate = diffRate;
    	this.isTournament = isTournament;
    	this.isHGreX = isHGreX;
    	this.isHybrid = isHybrid;
    	population = new ArrayList<>(nbIndividuals);
    	this.locations = locations;
    	rand = new Random();
    	selectionOperator = new SelectionOperator(this);
    	mutationOperator = new MutationOperator(this);
    	crossoverOperator = new CrossoverOperator(this);
    	transformationOperator = new TransformationOperator(this);
    	bestCostsHistory = new TreeMap<>();
	}
	
    public void exec() {
    	bestCostsHistory.clear();
    	initPopulation();
		displayDescription();
		int percentage = -1, newPercentage;
		ArrayList<Vehicle> p1, p2, c, randomInd, mutant;
		ArrayList<ArrayList<Vehicle>> selectedParents, childsOX;
		for (int i = 1; i <= nbGenerations; i++) {
			selectedParents = isTournament ? selectionOperator.tournamentSelection(3) : selectionOperator.rouletteWheelSelection();
			p1 = selectedParents.get(0);
			p2 = selectedParents.get(1);
			if (isHGreX) {
				c = crossoverOperator.hGreXCrossover(p1, p2);
			} else {
				childsOX = crossoverOperator.oXCrossover(p1, p2);
				c = childsOX.get(0);
				addConsideringSimilarities(isHybrid ? descent(childsOX.get(1)) : childsOX.get(1), i);
			}
			addConsideringSimilarities(isHybrid ? descent(c) : c, i);
			if (pMutation != 0 && rand.nextDouble() < pMutation) {
				randomInd = getRandomIndividualButNotBest();
				mutant = rand.nextDouble() < 0.5 ? mutationOperator.inversionMutation(randomInd) 
						: mutationOperator.displacementMutation(randomInd);
				if (isHybrid) {
					mutant = descent(mutant);
				}
				population.remove(randomInd);
				population.add(mutant);
				updateBestIndividual(mutant, objectiveFunction(mutant), i);
			}
			if ((newPercentage = (int)(((double)(i + 1) / nbGenerations) * 100)) != percentage) {
				System.out.println((percentage = newPercentage) + "%");
			}
		}
	    displayIndividual(bestIndividual);
    }
    
    private void initPopulation() {
    	population.clear();
		double minCost = Double.POSITIVE_INFINITY, currCost;
		ArrayList<Vehicle> minInd = null, individual;
		ArrayList<Location> locationsCopy = Util.createDeepCopyLocations(locations);
		locationsCopy.remove(Util.getLocationById(0, locationsCopy));
		for (int i = 0; i < nbIndividuals; i++) {
	        Collections.shuffle(locationsCopy);
	        individual = reconstruct(locationsCopy);
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
	
	private void addConsideringSimilarities(ArrayList<Vehicle> individual, int i) {
		double indCost = objectiveFunction(individual), currIndCost;
		ArrayList<Vehicle> worse;
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
		if (!hasSimilar && (worse = getRandomWorseIndividual(indCost)) != null) {
			population.remove(worse);
			population.add(individual);
			updateBestIndividual(individual, indCost, i);
		}
	}
	
	private boolean areSimilar(double a, double b, double perc) {
		return Math.abs(a - b) / Math.min(a, b) <= perc;
	}
	
	private ArrayList<Vehicle> getRandomIndividualButNotBest() {
		ArrayList<ArrayList<Vehicle>> notBest = new ArrayList<>();
		for (ArrayList<Vehicle> individual : population) {
			if (objectiveFunction(individual) != bestCost) {
				notBest.add(individual);
			}
		}
		return !notBest.isEmpty() ? notBest.get(rand.nextInt(notBest.size())) : population.get(0);
	}
	
	private ArrayList<Vehicle> getRandomWorseIndividual(double cost) {
		ArrayList<ArrayList<Vehicle>> worst = new ArrayList<>();
		for (ArrayList<Vehicle> individual : population) {
			if (cost < objectiveFunction(individual)) {
				worst.add(individual);
			}
		}
		return !worst.isEmpty() ? worst.get(rand.nextInt(worst.size())) : null;
	}
	
	private ArrayList<Vehicle> descent(ArrayList<Vehicle> individual) {
    	ArrayList<Vehicle> neighbor;
    	while ((neighbor = getBestNeighborAndBetter(individual)) != null) {
    		individual = neighbor;
    	}
    	return individual;
    }
    
    private ArrayList<Vehicle> getBestNeighborAndBetter(ArrayList<Vehicle> individual) {
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
						if ((newInd = isSameRoute ? transformationOperator.twoOptTransformation(individual, vFromIdx, locFromIdx, locToIdx) 
								: transformationOperator.swapTransformation(individual, routeFrom, routeTo, vFromIdx, vToIdx, locFromIdx, locToIdx)) != null 
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
	
	private void updateBestIndividual(ArrayList<Vehicle> individual, double cost, int i) {
		if (cost < bestCost) {
			bestCost = cost;
			bestIndividual = individual;
			bestCostsHistory.put(i, bestCost);
		}
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
		String description = "Co�t initial = " + (double) Math.round(bestCost * 1000) / 1000;
		description += "\nNombre de g�n�rations = " + nbGenerations;
		description += "\nNombre d'individus = " + nbIndividuals;
		description += "\nProbabilit� de mutation = " + pMutation;
		description += "\nTaux de diff�rence = " + diffRate;
		description += "\nOp�rateur de s�lection = " + (isTournament ? "Tournoi" : "Roulette");
		description += "\nOp�rateur de croisement = " + (isHGreX ? "HGreX" : "OX");
		description += "\n" + (isHybrid ? "Hybride" : "Non-Hybride");
		System.out.println(description);
		System.out.println("----------------------------------------------------------------------------------------------------");
	}
	
	public String getInlineDescription() {
		String description = "Co�t final = " + (double) Math.round(bestCost * 1000) / 1000;
		description += " | Nb v�hicules = " + bestIndividual.size() + " | ";
		description += " | Nb indvs = " +  nbIndividuals;
		description += " | Nb gens = " + nbGenerations;
		description += " | P(mutation) = " + pMutation;
		description += " | Taux diff = " + diffRate;
		description += " | S = " + (isTournament ? "Tournoi" : "Roulette");
		description += " | C = " + (isHGreX ? "HGreX" : "OX");
		description += " | " + (isHybrid ? "Hybride" : "Non-Hybride");
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
    
    public int getMaxCapacity() {
    	return maxCapacity;
    }
    
    public TreeMap<Integer, Double> getBestCostsHistory() {
    	return bestCostsHistory;
    }
    
    public Random getRand() {
    	return rand;
    }
    
    public int getNbIndividuals() {
    	return nbIndividuals;
    }
}
