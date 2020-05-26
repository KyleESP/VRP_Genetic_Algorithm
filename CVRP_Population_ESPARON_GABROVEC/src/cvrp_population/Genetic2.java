package cvrp_population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import operators.CrossoverOperator;
import operators.MutationOperator;
import operators.SelectionOperator;

public class Genetic2 {
	
	private CrossoverOperator crossoverOperator;
	private MutationOperator mutationOperator;
	private SelectionOperator selectionOperator;
	private long nbGenerations;
	private int nbIndividuals;
	private int nbBest;
	private double pMutation;
	private int maxCapacity;
	private ArrayList<Location> locations;
	private ArrayList<ArrayList<Vehicle>> population;
    private ArrayList<Vehicle> bestIndividual;
    private double bestCost;
    private ArrayList<Object[]> costsHistory;
	private Random rand;
	
	public Genetic2(ArrayList<Location> locations, int nbVehicles, int maxCapacity, long nbGenerations, int nbIndividuals, int nbBest, double pMutation) {
    	this.maxCapacity = maxCapacity;
    	this.nbGenerations = nbGenerations;
    	this.nbIndividuals = nbIndividuals;
    	this.nbBest = nbBest;
    	this.pMutation = pMutation;
    	population = new ArrayList<>(nbIndividuals);
    	this.locations = locations;
    	rand = new Random();
    	/*selectionOperator = new SelectionOperator(this);
    	mutationOperator = new MutationOperator(this);
    	crossoverOperator = new CrossoverOperator(this);*/
    	costsHistory = new ArrayList<>();
		initPopulation();
	}
    
    public void exec() {
		bestCost = Double.POSITIVE_INFINITY;
		updateBestIndividual(0);
		displayDescription();
		int percentage = -1, newPercentage;
		ArrayList<Vehicle> p1, p2;
		ArrayList<ArrayList<Vehicle>> reproductedPopulation, childs;
		for (int i = 0; i < nbGenerations; i++) {
			reproductedPopulation = selectionOperator.rouletteWheel();
			bestSolutionsReproduction();
			while (population.size() < nbIndividuals) {
				p1 = reproductedPopulation.get(rand.nextInt(reproductedPopulation.size()));
				if (rand.nextDouble() < pMutation) {
					population.add(rand.nextDouble() < 0.5 ? mutationOperator.displacementMutation(p1) : mutationOperator.inversionMutation(p1));
				} else {
					p2 = reproductedPopulation.get(rand.nextInt(reproductedPopulation.size()));
					childs = crossoverOperator.oxCrossover(p1, p2);
					population.add(childs.get(0));
					if (population.size() < nbIndividuals) {
						population.add(childs.get(1));
					}
				}
			}
			updateBestIndividual(i + 1);
			if ((newPercentage = (int)(((double)(i + 1) / nbGenerations) * 100)) != percentage) {
				percentage = newPercentage;
				System.out.println(percentage + "%");
			}
		}
		displaySolution(bestIndividual);
    }
    
    private void bestSolutionsReproduction() {
		population.sort((idv1, idv2) -> Double.compare(objectiveFunction(idv1), objectiveFunction(idv2)));
		ArrayList<ArrayList<Vehicle>> tmpPopulation = new ArrayList<>();
		for (int i = 0; i < nbBest; i++) {
			tmpPopulation.add(population.get(i));
		}
		population = tmpPopulation;
	}
	
    private void initPopulation() {
		for (int i = 0; i < nbIndividuals; i++) {
			ArrayList<Vehicle> vehicles = new ArrayList<>();
			population.add(vehicles);
	        int vIdx = 0;
	        Vehicle v;
	        ArrayList<Location> locationsCopy = Util.createDeepCopyLocations(locations);
	        Location depot = Util.getLocationById(0, locationsCopy);
	        Collections.shuffle(locationsCopy);
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
		boolean hasUnroutedLocation = false;
        for(Location l : locations) {
            if (!l.getIsRouted()) {
            	hasUnroutedLocation = true;
            	break;
            }
        }
        return hasUnroutedLocation;
    }
	
	private void updateBestIndividual(int i) {
		double fMin = Double.POSITIVE_INFINITY, fCurr;
		ArrayList<Vehicle> xMin = null;
		for (ArrayList<Vehicle> vehicles : population) {
			if ((fCurr = objectiveFunction(vehicles)) < fMin) {
				fMin = fCurr;
				xMin = vehicles;
			}
		}
		if (fMin < bestCost) {
			bestCost = fMin;
			bestIndividual = xMin;
			costsHistory.add(new Object[] {i, bestCost});
		}
	}
	
	public <T> ArrayList<Vehicle> reconstruct(ArrayList<T> brokenLocations) {
		ArrayList<Vehicle> newChild = new ArrayList<>();
		Vehicle v = new Vehicle(maxCapacity);
		Location depot = Util.getLocationById(0, locations), l;
		v.routeLocation(depot);
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
	
    public void displaySolution(ArrayList<Vehicle> vehicles) {
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
    
    public ArrayList<Object[]> getCostsHistory() {
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
    }
    
    public int getNbIndividuals() {
    	return nbIndividuals;
    }
}
