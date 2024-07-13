package macking;

import java.util.ArrayList;
import java.util.List;

public class Comida {

	private List<Producto> productos = new ArrayList<Producto>();
	
	public void addProducto(Producto producto) {
		productos.add(producto);		
	}

	@Override
	public String toString() {
		return "Comida [productos=" + productos + "]";
	}	
}
