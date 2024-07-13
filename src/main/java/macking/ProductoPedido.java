package macking;

public enum ProductoPedido {

	COCA_COLA(false,2), 
	NESTEA(false,2.5), 
	PATATAS_FRITAS(false,4), 
	HELADO(false,1.5), 
	WHOPPER(true,3), 
	BIGMAC(true,4), 
	NUGGETS(true,5), 
	PIZZA(true,10);

	private boolean elaborado;
	private double precio;

	private ProductoPedido(boolean elaborado, double precio) {
		this.elaborado = elaborado;
		this.precio = precio;
	}

	public boolean esElaborado() {
		return elaborado;
	}
	
	public double getPrecio() {
		return precio;
	}	
	
}
