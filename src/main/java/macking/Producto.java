package macking;

public class Producto {

	public ProductoPedido productoPedido;

	public Producto(ProductoPedido productoPedido) {
		this.productoPedido = productoPedido;
	}

	@Override
	public String toString() {
		return "Producto [" + productoPedido + "]";
	}

}
