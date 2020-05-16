package cvrp_population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Genetic {
	
	private long nbGenerations;
	private int nbIndividuals;
	private int nbBest;
	private double pCross;
	private int nbPointsCross;
	private int maxCapacity;
    private int nbVehicles;
	private ArrayList<Location> locations;
	private ArrayList<ArrayList<Vehicle>> population;
    private ArrayList<Vehicle> bestVehicles;
    private double bestCost;
    private ArrayList<Double> costsHistory;
	private Random rand;
	
	public Genetic(ArrayList<Location> locations, int nbVehicles, int maxCapacity, long nbGenerations, int nbIndividuals, int nbBest, double pCross, int nbPointsCross) {
		this.nbVehicles = nbVehicles;
    	this.maxCapacity = maxCapacity;
    	this.nbGenerations = nbGenerations;
    	this.nbIndividuals = nbIndividuals;
    	this.nbBest = nbBest;
    	this.pCross = pCross;
    	this.nbPointsCross = nbPointsCross;
    	population = new ArrayList<ArrayList<Vehicle>>(nbIndividuals);
    	this.locations = Util.createDeepCopyLocations(locations);
    	rand = new Random();
    	costsHistory = new ArrayList<>();
		initPopulation();
	}
    
    public ArrayList<Vehicle> getBestVehicles() {
    	return bestVehicles;
    }
    
    public double getBestCost() {
    	return bestCost;
    }
    
    public ArrayList<Double> getCostsHistory() {
    	return costsHistory;
    }
	
	public void exec() {
		hGreXCrossover(population.get(1), population.get(2));
		System.exit(0);
		bestCost = Double.POSITIVE_INFINITY;
		updateBestSolution();
		//displayDescription();
		for (int i = 0; i < nbGenerations; i++) {
			ArrayList<ArrayList<Vehicle>> populationTournament = tournament();
			ArrayList<Vehicle> p1 = populationTournament.get(0);
			ArrayList<Vehicle> p2 = populationTournament.get(1);
			ArrayList<Vehicle> c = hGreXCrossover(p1, p2);
			updateBestSolution();
		}
	    //displayBestSolution();
    }
	
	private ArrayList<ArrayList<Vehicle>> tournament() {
		ArrayList<ArrayList<Vehicle>> newPopulation = new ArrayList<>();
		ArrayList<ArrayList<Vehicle>> populationCopy = Util.createDeepCopyPopulation(population);
		int populationSize = populationCopy.size();
		int idxIdv1, idxIdv2;
		boolean bestIdvWin;
		double p = 0.85;
		ArrayList<Vehicle> idv1, idv2;
		while (!populationCopy.isEmpty()) {
			if (populationCopy.size() == 1) {
				newPopulation.add(populationCopy.get(0));
				break;
			}
			idxIdv1 = rand.nextInt(populationSize);
			idv1 = populationCopy.get(idxIdv1);
			populationCopy.remove(idxIdv1);
			idxIdv2 = rand.nextInt(populationSize);
			idv2 = populationCopy.get(idxIdv2);
			populationCopy.remove(idxIdv2);
			bestIdvWin = rand.nextDouble() < p;
			if (objectiveFunction(idv1) <= objectiveFunction(idv2)) {
				newPopulation.add(bestIdvWin ? idv1 : idv2);
			} else {
				newPopulation.add(bestIdvWin ? idv2 : idv1);
			}
		}
		return newPopulation;
	}
	
	private ArrayList<Vehicle> hGreXCrossover(ArrayList<Vehicle> p1, ArrayList<Vehicle> p2) {
		ArrayList<Location> p1Locations = getLocations(p1);
		HashMap<int[], Double> pCosts = getEdgesCosts(p1Locations);
		ArrayList<Location> p2Locations = getLocations(p2);
		pCosts.putAll(getEdgesCosts(p2Locations));
		ArrayList<Integer> child = new ArrayList<>();
		child.add(p1Locations.get(0).getId());
		int lastLocId = p1Locations.get(1).getId();
		child.add(lastLocId);
		Double min;
		int[] minEdge;
		HashMap<Integer, HashMap<Integer, Double>> distances = Util.getDistances();
		while (child.size() < p1Locations.size()) {
			min = Double.POSITIVE_INFINITY;
			minEdge = null;
			for (Map.Entry<int[], Double> me : pCosts.entrySet()) {
				if (me.getKey()[0] == lastLocId && !child.contains(me.getKey()[1]) && me.getValue() < min) {
					minEdge = me.getKey();
					min = me.getValue();
				}
	        }
			if (minEdge == null) {
				Double distance;
				for (Location l : p1Locations) {
					if (!child.contains(l.getId()) && (distance = distances.get(lastLocId).get(l.getId())) < min) {
						minEdge = new int[] {lastLocId, l.getId()};
						min = distance;
					}
				}
			}
			lastLocId = minEdge[1];
			child.add(lastLocId);
		}
		ArrayList<Vehicle> newChild = new ArrayList<>();
		Vehicle v = new Vehicle(maxCapacity);
		Location depot = getLocationById(0);
		v.routeLocation(depot);
		for (int i = 0; i < child.size(); i++) {
			if (!v.routeLocation(getLocationById(child.get(i)))) {
				v.routeLocation(depot);
				newChild.add(v);
				v = new Vehicle(maxCapacity);
				v.routeLocation(depot);
				v.routeLocation(getLocationById(child.get(i)));
			} else if (i == child.size() - 1) {
				v.routeLocation(depot);
				newChild.add(v);
			}
		}
		
		return newChild;
	}
	
	private Location getLocationById(int id) {
		for (Location l : locations) {
			if (l.getId() == id) {
				return l;
			}
		}
		return null;
	}
	private HashMap<int[], Double> getEdgesCosts(ArrayList<Location> locations) {
		HashMap<int[], Double> edgesCosts = new HashMap<>();
		HashMap<Integer, HashMap<Integer, Double>> distances = Util.getDistances();
		int idSource, idDest;
		double distance;
		for (int i = 0; i < locations.size() - 1; i++) {
			idSource = locations.get(i).getId();
			idDest = locations.get(i + 1).getId();
			int[] edge = new int[] {idSource, idDest};
			distance = distances.get(idSource).get(idDest);
			edgesCosts.put(edge, distance);
		}
		return edgesCosts;
	}
	private ArrayList<Location> getLocations(ArrayList<Vehicle> individual) {
		ArrayList<Location> locations = new ArrayList<>();
		for (Vehicle v : individual) {
			for (Location l : v.getRoute()) {
				if (l.getId() != 0) {
					locations.add(l);
				}
			}
		}
		return locations;
	}
	
	private ArrayList<ArrayList<Vehicle>> mutation(ArrayList<ArrayList<Vehicle>> population) {
		return null;
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
			bestVehicles = xMin;
		}
    	costsHistory.add(fMin);
	}
	
	public void displayDescription() {
		System.out.println("----------------------------------------------------------------------------------------------------");
		System.out.println("Algorithme g�n�tique :");
		String description = "Co�t initial = " + (double) Math.round(bestCost * 1000) / 1000;
		description += "\nNombre de g�n�rations = " + nbGenerations;
		description += "\nNombre d'individus = " + nbIndividuals;
		description += "\nProbabilit� de croisement = " + pCross;
		System.out.println(description);
		System.out.println("----------------------------------------------------------------------------------------------------");
	}
	
	public String getInlineDescription() {
		String description = "Co�t final = " + (double) Math.round(bestCost * 1000) / 1000;
		description += " | Nb v�hicules = " + bestVehicles.size() + " | ";
		description += " | Nombre d'individus = " +  nbIndividuals;
		description += " | Nombre de g�n�rations = " + nbGenerations;
		description += " | Probabilit� de croisement = " + pCross;
		return description;
	}
	
	private double objectiveFunction(ArrayList<Vehicle> vehicles) {
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
    
    public void displaySolution(ArrayList<Vehicle> vehicles) {
    	double cost = objectiveFunction(vehicles);
    	System.out.println("----------------------------------------------------------------------------------------------------");
        for (int i = 0 ; i < vehicles.size() ; i++) {
            System.out.println("V�hicule n�" + (i + 1) + " : " + getRouteString(vehicles.get(i).getRoute()));
        }
        System.out.println("\nCo�t de la solution : " + cost);
    	System.out.println("----------------------------------------------------------------------------------------------------");
    }
    
    private String getRouteString(ArrayList<Location> route) {
    	int routeSize = route.size();
    	String routeString = "";
    	for (int i = 0; i < routeSize ; i++) {
        	routeString += "(" + route.get(i).getId() + ")" + ((i != routeSize - 1) ? " == " : "");
        }
    	return routeString;
    }
    
    public void displayPopulation(ArrayList<ArrayList<Vehicle>> population) {
    	for (int i = 0; i < population.size(); i++) {
    		System.out.println("Individu n�" + (i + 1));
    		displaySolution(population.get(i));
    		System.out.println();
    	}
    }
    
    public ArrayList<ArrayList<Vehicle>> getPopulation() {
    	return population;
    }
}
