package macking;

public class Ticket {

	private Pedido pedido;
	
	public Ticket(Pedido pedido) {
		this.pedido = pedido;
	}
	
	public Pedido getPedido() {
		return pedido;
	}

}
