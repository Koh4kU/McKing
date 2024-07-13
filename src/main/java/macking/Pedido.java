package macking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Pedido {

	private List<ProductoPedido> productosPedido = new ArrayList<ProductoPedido>();
	private int numPedido;
	
	public static Pedido crearCualquierPedido() {
		
		Random random = new Random();
		
		int numProductos = random.nextInt(10)+1;
		
		Pedido pedido = new Pedido();
		
		for(int i=0; i<numProductos; i++){
			int randomNumProduct = random.nextInt(ProductoPedido.values().length);
			ProductoPedido randomProduct = ProductoPedido.values()[randomNumProduct];
			pedido.productosPedido.add(randomProduct);
		}
		
		return pedido;
	}

	public double calcularImporte() {
		double importe = 0;
		for(ProductoPedido productoPedido : productosPedido){
			importe += productoPedido.getPrecio();
		}
		return importe;
	}

	public List<ProductoPedido> getProductosPedido() {
		return productosPedido;
	}

	public int getNum() {
		return numPedido;		
	}
	
	public void setNum(int numPedido){
		this.numPedido = numPedido;
	}

}
