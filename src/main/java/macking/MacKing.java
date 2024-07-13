package macking;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class MacKing {

	private static int NUM_CLIENTES;
	private static int NUM_EMP_COGE_PEDIDOS;
	private static int NUM_EMP_ELABORA_PRODUCTOS;
	private static int NUM_EMP_PREPARA_PEDIDO;
	private static int NUM_MESAS;


	private static AtomicInteger numPedido;
	private static AtomicInteger numClientesCola;
	private Semaphore abiertoEmpleados=new Semaphore(0);
	private Semaphore abiertoClientes=new Semaphore(0);
	private Semaphore sCobrar[];
	private Exchanger<Pedido>[] ePedido;
	private Exchanger<Ticket>[] eTicket;
	private Exchanger<Double>[] eImporte;
	private Semaphore sPedir;
	private static AtomicBoolean isFinJornada=new AtomicBoolean(false);
	private static volatile Thread[] clientesEnCaja;
	private Semaphore emPedir=new Semaphore(1);
	//elaborar
	private Exchanger<Pedido> exElaborarPedido[];
	private BlockingQueue<Pedido> listaPedidos;
	//preparar
	private Exchanger<Ticket> exPrepararTicket[];
	private Exchanger<Comida> exPrepararComida[];
	private BlockingQueue<Producto>[] carrilesProductos =new LinkedBlockingQueue[4];
	private static volatile Thread[] clientesEnPreparar;
	private Semaphore sPreparar;
	private Semaphore emEntregarticket=new Semaphore(1);
	//mesas
	private static AtomicInteger numMesasLimpias;
	private static volatile String[] mesas;

	private BlockingQueue<Integer> mesasLimpias;
	private BlockingQueue<Integer> mesasSucias;

	//Cerrar
	private static volatile List<Thread> clientesEnEstablecimiento=new LinkedList<>();
	private static volatile List<Thread> empleadosEnEstablecimiento=new LinkedList<>();
	private CountDownLatch countDownAbiertoClientes=new CountDownLatch(1);
	private CountDownLatch countDownAbiertoEmpleados=new CountDownLatch(1);

	public MacKing(int numMesas, int num_empCogePedidos, int numEmpElaboraProductos, int numEmpPreparaProductos) {

		NUM_MESAS=numMesas;
		NUM_EMP_ELABORA_PRODUCTOS=numEmpElaboraProductos;
		NUM_EMP_PREPARA_PEDIDO=numEmpPreparaProductos;
		NUM_EMP_COGE_PEDIDOS=num_empCogePedidos;

		ePedido=new Exchanger[NUM_EMP_COGE_PEDIDOS];
		eTicket=new Exchanger[NUM_EMP_COGE_PEDIDOS];
		eImporte=new Exchanger[NUM_EMP_COGE_PEDIDOS];
		sPedir=new Semaphore(NUM_EMP_COGE_PEDIDOS);
		clientesEnCaja=new Thread[NUM_EMP_COGE_PEDIDOS];
		sCobrar=new Semaphore[NUM_EMP_COGE_PEDIDOS];
		for(int i=0;i<NUM_EMP_COGE_PEDIDOS;i++){
			sCobrar[i]=new Semaphore(0);
		}
		//elaborar
		exElaborarPedido=new Exchanger[NUM_EMP_ELABORA_PRODUCTOS];
		listaPedidos=new LinkedBlockingQueue<Pedido>();
		//preparar
		exPrepararTicket=new Exchanger[NUM_EMP_PREPARA_PEDIDO];
		exPrepararComida=new Exchanger[NUM_EMP_PREPARA_PEDIDO];
		clientesEnPreparar=new Thread[NUM_EMP_PREPARA_PEDIDO];
		sPreparar =  new Semaphore(NUM_EMP_PREPARA_PEDIDO);
		//mesas
		mesas=new String[NUM_MESAS];
		//Con colas
		mesasLimpias=new LinkedBlockingQueue<>(NUM_MESAS);
		mesasSucias=new LinkedBlockingQueue<>();
		for(int i=0;i<NUM_MESAS;i++)
			mesasLimpias.add(i);

		//cerrar

		numClientesCola=new AtomicInteger(0);
		numPedido=new AtomicInteger(0);
		numMesasLimpias=new AtomicInteger(numMesas);

		for(int i=0;i<NUM_EMP_COGE_PEDIDOS;i++) {
			ePedido[i] = new Exchanger<Pedido>();
			eTicket[i] = new Exchanger<Ticket>();
			eImporte[i] = new Exchanger<Double>();
			clientesEnCaja[i] = null;
		}
		for(int i=0;i<NUM_MESAS;i++){
			mesas[i]="mesaLimpia";
		}
		for(int i=0;i<NUM_EMP_PREPARA_PEDIDO;i++){
			exPrepararComida[i]=new Exchanger<Comida>();
			exPrepararTicket[i]=new Exchanger<Ticket>();
			clientesEnPreparar[i]=null;
		}
		for(int i=0;i<NUM_EMP_ELABORA_PRODUCTOS;i++){
			exElaborarPedido[i]=new Exchanger<Pedido>();
		}
		//de 4 porque hay 4 productos elaborados
		for(int i=0;i<4;i++){
			carrilesProductos[i] = new LinkedBlockingQueue<Producto>();
		}
	}

	public void entrarComoCliente() {
		//hay que ver si esta abierto
		try {
			countDownAbiertoClientes.await();
			abiertoClientes.acquire();
			clientesEnEstablecimiento.add(currentThread());
			System.out.println(currentThread().getName()+": entrando");
			//sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void entrarComoEmpleado() {
		try {
			countDownAbiertoEmpleados.await();
			abiertoEmpleados.acquire();
			empleadosEnEstablecimiento.add(currentThread());
			//sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void abrirParaEmpleados() {
		abiertoEmpleados.release(17);
		numPedido.set(0);
		isFinJornada.set(false);
		countDownAbiertoEmpleados.countDown();
	}

	public void abrirParaClientes() {
		abiertoClientes.release(10);
		countDownAbiertoClientes.countDown();
	}

	public void hacerColaSiHayGente() {
		numClientesCola.incrementAndGet();
		try {
			sPedir.acquire();
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public void hacerPedido(Pedido pedido) {
		numClientesCola.decrementAndGet();
		numPedido.incrementAndGet();
		pedido.setNum(numPedido.get());
		try {
			emPedir.acquire();
		} catch (InterruptedException e) {e.printStackTrace();}
		for(int i =0;i<NUM_EMP_COGE_PEDIDOS;i++){
			if(clientesEnCaja[i]==null) {
				clientesEnCaja[i] = currentThread();
				emPedir.release();
				try {
					ePedido[i].exchange(pedido);
				} catch (InterruptedException e) {e.printStackTrace();}
				break;
			}
		}
	}

	public double esperarImporte() {
		Double importe=0.0;
		try {
			for(int i=0;i<NUM_EMP_COGE_PEDIDOS;i++){
				if(clientesEnCaja[i]==currentThread()){
					 importe=eImporte[i].exchange(null);
					 break;
				}
			}
		} catch (InterruptedException e) {e.printStackTrace();}
		return importe;
	}
	public void cancelarPedido(){
		for(int i=0;i<NUM_EMP_COGE_PEDIDOS;i++){
			if(clientesEnCaja[i]==currentThread()){
				clientesEnCaja[i]=null;
				System.out.println(currentThread().getName()+": Pedido cancelado");
				sCobrar[i].release();
				break;
			}
		}
		sPedir.release();
	}

	public Ticket pagarYRecogerTicket() {
		Ticket t=null;
		System.out.println(currentThread().getName()+": cogiendo ticket");
		for(int i=0;i<NUM_EMP_COGE_PEDIDOS;i++){
			if(currentThread()==clientesEnCaja[i]){
				try {
					sCobrar[i].release();
					t=eTicket[i].exchange(null);
				} catch (InterruptedException e) {e.printStackTrace();}
				System.out.println(currentThread().getName()+" tome señor : EmpCogePedidos_"+i);
				clientesEnCaja[i]=null;
				break;
			}
		}
		sPedir.release();
		return t;
	}

	public Comida esperarPedido(Ticket ticket) {
		Comida comida=new Comida();
		try {
			sPreparar.acquire();
			//al igual que en la caja de clientes hay que asegurarse de que dos clientes no den el ticket al mismo empleado a la vez
			emEntregarticket.acquire();
		} catch (InterruptedException e) {e.printStackTrace();}
		for(int i=0;i<NUM_EMP_PREPARA_PEDIDO;i++){
			if(clientesEnPreparar[i]==null){
				clientesEnPreparar[i]=currentThread();
				emEntregarticket.release();
				try {
					exPrepararTicket[i].exchange(ticket);
					//esperar comida
					comida=exPrepararComida[i].exchange(null);
					//dejar vacio el puesto
					clientesEnPreparar[i]=null;
					sPreparar.release();
				} catch (InterruptedException e) {e.printStackTrace();}
				break;
			}
		}
		return comida;
	}

	public void sentarseEnMesa() {
		int mesa=-1;
		try {
			mesa=mesasLimpias.take();
			numMesasLimpias.decrementAndGet();
		} catch (InterruptedException e) {e.printStackTrace();}
		mesas[mesa]= currentThread().getName();
	}

	public void dejarMesa() {
		for(int i=0;i<NUM_MESAS;i++){
			if(mesas[i]== currentThread().getName()){
				mesas[i]="mesaSucia";
				System.out.println(currentThread().getName()+": dejando mesa "+i);
				try {
					mesasSucias.put(i);
				} catch (InterruptedException e) {e.printStackTrace();}
				break;
			}
		}

	}



	public Integer getNumClientesCola() {
		return numClientesCola.get();
	}

	public void echarElCierre() {
		System.out.println(currentThread().getName()+": voy a cerrar");
		countDownAbiertoClientes=new CountDownLatch(1);
		//esperar clientes a que salgan
		try {
			abiertoClientes.acquire(10);
			System.out.println(currentThread().getName()+": Ya han salido todos los clientes, cierro");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void cerrar() {
		countDownAbiertoEmpleados=new CountDownLatch(1);
		isFinJornada.set(true);
		if(!empleadosEnEstablecimiento.isEmpty()){
			for(int i=0;i<empleadosEnEstablecimiento.size();i++){
				empleadosEnEstablecimiento.get(i).interrupt();
			}
		}
	}

	public int buscarMesaConComida() {
		int mesa=-1;
		try {
			mesa=mesasSucias.take();
			mesasLimpias.put(mesa);
			numMesasLimpias.incrementAndGet();
		} catch (InterruptedException e) {e.printStackTrace();}
		return mesa;
	}

	public boolean esFinJornada() {
		return isFinJornada.get();
	}


	public Pedido esperarPedidoCliente() {
		Pedido pedido=null;
		int numeroCaja= Integer.parseInt(currentThread().getName().split("_")[1]);
		try {
			pedido=ePedido[numeroCaja].exchange(null);
		} catch (InterruptedException e) {e.printStackTrace();}

		return pedido;
	}

	//Se ha modificado la salida de este metodo, para saber si el cliente se ha ido sin pagar por que no tenia dinero
	public boolean indicarImporteYCobrar(double importe, Pedido pedido) {
		int numeroCaja = Integer.parseInt(currentThread().getName().split("_")[1]);
		String nombreCliente=clientesEnCaja[numeroCaja].getName();
		System.out.println(Thread.currentThread().getName() + ": " + "Serian: " + importe + " señor: " + clientesEnCaja[numeroCaja].getName());
		try {
			eImporte[numeroCaja].exchange(importe);
			sCobrar[numeroCaja].acquire();
		} catch (InterruptedException e) {e.printStackTrace();}
		if (clientesEnCaja[numeroCaja]!=null && clientesEnCaja[numeroCaja].getName()==nombreCliente){
			Ticket ticket = new Ticket(pedido);
			try {
				eTicket[numeroCaja].exchange(ticket);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		}
		else{
			return false;
		}

	}

	public void tramitarPedido(Pedido pedido) {
		try {
			listaPedidos.put(pedido);
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public void salir() {
		if(currentThread().getName().contains("Emp")){
			empleadosEnEstablecimiento.remove(currentThread());
			abiertoEmpleados.release();
			System.out.println(currentThread().getName()+": saliendo");
		}
		else{
			clientesEnEstablecimiento.remove(currentThread());
			abiertoClientes.release();
			System.out.println(currentThread().getName()+": saliendo");
		}

	}

	public Pedido esperarPedidoParaElaborar() {
		Pedido pedido=null;
		try {
			pedido=listaPedidos.take();
		} catch (InterruptedException e) {e.printStackTrace();}
		return pedido;
	}

	public void enviarComidaAPreparador(Comida comida) {
	}

	public void dejarProductoEnCarriles(Producto producto) {
		System.out.println(currentThread().getName()+": dejando en el carril el producto: "+producto.productoPedido.name());
		try{
			switch(producto.productoPedido.name()){
				case "WHOPPER":
					carrilesProductos[0].put(producto);
					break;
				case "BIGMAC":
					carrilesProductos[1].put(producto);
					break;
				case "NUGGETS":
					carrilesProductos[2].put(producto);
					break;
				case "PIZZA":
					carrilesProductos[3].put(producto);
					break;
			}
		}catch (InterruptedException e) {e.printStackTrace();}
	}


	public Ticket esperarClienteConTicket() {
		int numeroCajaPreparar = Integer.parseInt(currentThread().getName().split("_")[1]);
		Ticket t=null;
		try {
			t=exPrepararTicket[numeroCajaPreparar].exchange(null);

		} catch (InterruptedException e) {e.printStackTrace();}
		return t;
	}

	public Producto cogerProductoDeCarriles(ProductoPedido productoPedido) {
		Producto producto=null;
		try{
			switch(productoPedido.name()){
				case "WHOPPER":
					producto=carrilesProductos[0].take();
					break;
				case "BIGMAC":
					producto=carrilesProductos[1].take();
					break;
				case "NUGGETS":
					producto=carrilesProductos[2].take();
					break;
				case "PIZZA":
					producto=carrilesProductos[3].take();
					break;
			}
		}catch (InterruptedException e) {e.printStackTrace();}
		return producto;
	}

	public void darComidaACliente(Comida comida) {
		int numeroCajaPreparar = Integer.parseInt(currentThread().getName().split("_")[1]);
		try {
			exPrepararComida[numeroCajaPreparar].exchange(comida);
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public Integer getMesasLibres() {
		return numMesasLimpias.get();
	}

}
