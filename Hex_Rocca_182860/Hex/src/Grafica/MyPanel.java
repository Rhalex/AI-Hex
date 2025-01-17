package Grafica;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import Logica.AI;
import Logica.BoardGame;
import Logica.DisturbingHex;
import Logica.Hexagon;

public class MyPanel extends JPanel {

	private BoardGame bg = new BoardGame();
	private Vector<BufferedImage> img = new Vector<BufferedImage>();
	private ArrayList<Point> yCoo = new ArrayList<Point>();
	private ArrayList<Float> xCoo = new ArrayList<Float>();
	private HashMap<Integer, HexDrawable> hexes;
	private AI pc;
	
	
	public MyPanel()
	{
		super();
		this.setBackground(Color.WHITE);
		
		hexes = new HashMap<>();
		
		yCoordinates();
		xCoordinates();
		
		MyMouseListener ml = new MyMouseListener();
		this.addMouseListener(ml);
		
		pc = new AI();
		
		upload();
	}
	
	public void play()
	{
		bg.CreateBoardGame();
		Draw(getGraphics());
	}
	
	public void Draw(Graphics g)
	{
		g.drawImage(img.get(0), 30, 100, null);
		
		for (Map.Entry<Integer, HexDrawable> entry : hexes.entrySet()) 
		{
			
			g.setColor(entry.getValue().color);
	        g.fillOval(entry.getValue().x, entry.getValue().y, 30, 30);
	        g.drawString(String.valueOf(entry.getValue().idLogicalHex), entry.getValue().x, entry.getValue().y);
	    }
		
		
		if(!hexes.isEmpty())
		{ 
			HexDrawable hex = hexes.get(hexes.size());
			g.setColor(Color.MAGENTA);
			g.fillOval(hex.x, hex.y, 30, 30);
		}
	}
	
	public void upload()
	{
		BufferedImage i = null;
		try {
		
			//i = ImageIO.read(new File("images/boardgame.png"));
			i = ImageIO.read(getClass().getResource("/boardgame.png"));
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		img.add(i);
	}
	
	class MyMouseListener extends MouseAdapter	
	{	
		public void mouseClicked(MouseEvent e)
		{ 
			//CONTROLLA CHE IL CLICK SIA AVVENUTO NELL'AREA DI GIOCO
			if(isValidClick(e.getX(), e.getY()))
			{
				String idHexClicked = pxToHex(e.getX(), e.getY() );
				//System.out.println(idHexClicked);
				
				if(idHexClicked!="X")
				{
					//CONTROLLA DI NON AVER CLICKATO UN ESAGONO NEMICO(red) O UN ESAGONO GIÀ CLICKATO(blue)
					if(bg.get(Integer.valueOf(idHexClicked)-1).getColor() == "white")
					{	
						//CREA L'ESAGONO(GRAFICO)
						hexToPx(idHexClicked, Color.BLUE);
						
						//AGGIORNA IL COLORE DELL'ESAGONO(LOGICO) DENTRO LA BOARDGAME
						bg.get(Integer.valueOf(idHexClicked)-1).setColor("blue");
						
						Draw(getGraphics());
						
						//MANDA IL NUOVO STATO ALL'AI
						for(int i=0; i<bg.size(); i++)
						{
							try 
							{
								pc.newFacts(bg.get(i));
							}
							catch (Exception e1) 
							{	
								e1.printStackTrace();
							}
						}
						
						//L'AI CONTROLLA LA PRESENZA DI UN VINCITORE
						String vincitore = null;
						try 
						{
							 vincitore = pc.verificaVincitore();
								 
						} 
						catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException e2) 
						{
							e2.printStackTrace();
						}
						
						if(vincitore != null)
						{
							JOptionPane.showMessageDialog(null, vincitore + " VINCE!", "WINNER", JOptionPane.INFORMATION_MESSAGE);
							System.exit(0);
						}
						else
						{
						
							//NUOVA FEATURE DEL PROF
							//CALCOLA LA DISTANZA MINIMA TRA LE CELLE BLUE E E I BORDI
							int idLastBlueHex = Integer.valueOf(idHexClicked);
							List<DisturbingHex> disturbingHexes = new ArrayList<DisturbingHex>();
							
							if(idLastBlueHex<56)
								disturbingHexes = calcolaHexDistrubantiUp();
							
							else if(idLastBlueHex>66)
								disturbingHexes = calcolaHexDistrubantiDown();
							
							else
							{
								disturbingHexes = calcolaHexDistrubantiUp();
								disturbingHexes.addAll(calcolaHexDistrubantiDown());	
							}
							
							try {
								
								for(int i=0; i<disturbingHexes.size(); i++)
									pc.newFacts(disturbingHexes.get(i));
								
							} catch (Exception e3) {
								
								e3.printStackTrace();
							}
							
							//PASSA IL TURNO ALL'AI
							String idPC = "-1";
							try 
							{
								idPC = pc.AI_Move();
							} 
							catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException e1)
							{
								e1.printStackTrace();
							}
							
							//System.out.println(idPC);
							//System.out.println("\nA");
							//pc.stampaFatti();
							
							//CREO ESAGONO(GRAFICO)
							hexToPx(idPC, Color.RED);
							
							//AGGIORNA IL COLORE DELL'ESAGONO(LOGICO) DENTRO LA BOARDGAME
							if(idPC!= "-1") //QUANDO RIMANE UNA SOLA CELLA ED È L'UMANO A CLICKARE IL PC RESTITUISCE null. GESTISCI IL CASO
								bg.get(Integer.valueOf(idPC)-1).setColor("red");
							
							//MANDO I FATTI ALL'AI.
							//ORA VEDE LA SUA ULTIMA MOSSA
							for(int i=0; i<bg.size(); i++)
							{
								try 
								{
									pc.newFacts(bg.get(i));
								}
								catch (Exception e1) 
								{	
									e1.printStackTrace();
								}
							}
							
							//CONTROLLA LA PRESENZA DI UN VINCITORE
							try 
							{
								 vincitore = pc.verificaVincitore();
								 Draw(getGraphics());
								 
								 pc.clearFacts();
									 
							} 
							catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException e2) 
							{
								e2.printStackTrace();
							}
							
							if(vincitore != null)
							{
								JOptionPane.showMessageDialog(null, vincitore + " VINCE!", "WINNER", JOptionPane.INFORMATION_MESSAGE);
								System.exit(0);
							}
						}
						
					}
				}	
			}
		}	
	}
	
	public List<DisturbingHex> calcolaHexDistrubantiDown()
	{
		List<DisturbingHex> dx = new ArrayList<DisturbingHex>();
		int min = 999999;
		for(Hexagon hex : bg)
		{
			if(hex.getColor().equals("blue"))
			{
				boolean exit = false;
				List<DisturbingHex> d = new ArrayList<DisturbingHex>();
				
				while(!exit)
				{
					Hexagon toAdd = null;
					
					if((bg.get(hex.getId()-1).getL5() - 1)>0 && !(d.contains(bg.get(bg.get(hex.getId()-1).getL5() - 1))) && bg.get(bg.get(hex.getId()-1).getL5() - 1).getColor().equals("white") )
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL5() - 1));
						
					else if((bg.get(hex.getId()-1).getL4() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL4() - 1)) && bg.get(bg.get(hex.getId()-1).getL4() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL4() - 1));
					
					else if((bg.get(hex.getId()-1).getL3() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL3() - 1)) && bg.get(bg.get(hex.getId()-1).getL3() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL3() - 1));
					
					else if((bg.get(hex.getId()-1).getL6() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL6() - 1)) && bg.get(bg.get(hex.getId()-1).getL6() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL6() - 1));
					
					else if((bg.get(hex.getId()-1).getL2() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL2() - 1)) && bg.get(bg.get(hex.getId()-1).getL2() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL2() - 1));
					
					else if((bg.get(hex.getId()-1).getL1() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL1() - 1)) && bg.get(bg.get(hex.getId()-1).getL1() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL1() - 1));
					
					else
						exit = true;
					
					if(!exit)
					{
						DisturbingHex temp = new DisturbingHex(toAdd);
						d.add(temp);
						
						hex = toAdd;
						
						if(hex.getId()==111 || hex.getId()==112 || hex.getId()==113 || hex.getId()==114 || hex.getId()==115 || hex.getId()==116 || hex.getId()==117 || hex.getId()==118 || hex.getId()==119 || hex.getId()==120 || hex.getId()==121)
							exit = true;
					}
				}
				
				if(d.size()>0 && d.size()<min)
				{
					dx.clear();
					dx = d;
					min = d.size();
				}
				
			}
		}
		
		return dx;

	}
	
	public List<DisturbingHex> calcolaHexDistrubantiUp()
	{
		List<DisturbingHex> dx = new ArrayList<DisturbingHex>();
		int min = 999999;
		for(Hexagon hex : bg)
		{
			if(hex.getColor().equals("blue"))
			{
				boolean exit = false;
				List<DisturbingHex> d = new ArrayList<DisturbingHex>();
				
				while(!exit)
				{
					Hexagon toAdd = null;
					
					if((bg.get(hex.getId()-1).getL1() - 1)>0 && !(d.contains(bg.get(bg.get(hex.getId()-1).getL1() - 1))) && bg.get(bg.get(hex.getId()-1).getL1() - 1).getColor().equals("white") )
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL1() - 1));
						
					else if((bg.get(hex.getId()-1).getL2() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL2() - 1)) && bg.get(bg.get(hex.getId()-1).getL2() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL2() - 1));
					
					else if((bg.get(hex.getId()-1).getL3() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL3() - 1)) && bg.get(bg.get(hex.getId()-1).getL3() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL3() - 1));
					
					else if((bg.get(hex.getId()-1).getL6() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL6() - 1)) && bg.get(bg.get(hex.getId()-1).getL6() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL6() - 1));
					
					else if((bg.get(hex.getId()-1).getL5() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL5() - 1)) && bg.get(bg.get(hex.getId()-1).getL5() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL5() - 1));
					
					else if((bg.get(hex.getId()-1).getL4() - 1)>0 && !d.contains(bg.get(bg.get(hex.getId()-1).getL4() - 1)) && bg.get(bg.get(hex.getId()-1).getL4() - 1).getColor().equals("white"))
						toAdd = new Hexagon(bg.get(bg.get(hex.getId()-1).getL4() - 1));
					
					else
						exit = true;
					
					if(!exit)
					{
						DisturbingHex temp = new DisturbingHex(toAdd);
						d.add(temp);
						
						hex = toAdd;
						
						if(hex.getId()==1 || hex.getId()==2 || hex.getId()==3 || hex.getId()==4 || hex.getId()==5 || hex.getId()==6 || hex.getId()==7 || hex.getId()==8 || hex.getId()==9 || hex.getId()==10 || hex.getId()==11)
							exit = true;
					}
				}
				
				if(d.size()>0 && d.size()<min)
				{
					dx.clear();
					dx = d;
					min = d.size();
				}
				
			}
		}
		
		return dx;
	}
	
	public String pxToHex(float x, float y)
	{
		int index = yRangeIndex((int) y);
		
		if(index==-1)
			return "X";
		
		float id = ((x - xCoo.get(index))/(float) ((49+4.7)) + 11*index )+1;
		
		return String.valueOf( (int) id);
	}
	
	public void hexToPx(String id, Color c)
	{
		int d = Integer.valueOf(id) - 1;
		int index = (int) d/11;
		
		int cx = (int) (xCoo.get(index)+(d-(11*index))*(49+4.7));
		int cy = (int) yCoo.get(index).x;
		
	
		hexes.put(hexes.size()+1, new HexDrawable(cx+8, cy, c, Integer.valueOf(id)));
		
	}
	
	public void yCoordinates()
	{
		yCoo.add(new Point(150, 178));
		yCoo.add(new Point(195, 224));
		yCoo.add(new Point(243, 271));
		yCoo.add(new Point(289, 317));
		yCoo.add(new Point(336, 364));
		yCoo.add(new Point(383, 411));
		yCoo.add(new Point(429, 457));
		yCoo.add(new Point(476, 504));
		yCoo.add(new Point(522, 550));
		yCoo.add(new Point(567, 597));
		yCoo.add(new Point(614, 642));
	}
	
	public void xCoordinates()
	{
		
		xCoo.add(new Float(356.0));
		xCoo.add(new Float(329.0));
		xCoo.add(new Float(302.0));
		xCoo.add(new Float(275.0));
		xCoo.add(new Float(248.0));
		xCoo.add(new Float(222.0));
		xCoo.add(new Float(195.0));
		xCoo.add(new Float(168.0));
		xCoo.add(new Float(141.0));
		xCoo.add(new Float(114.0));
		xCoo.add(new Float(87.0));
	}
	
	public int yRangeIndex(int y)
	{
		int i=0;
		for(; i<yCoo.size(); i++)
			if( y>=yCoo.get(i).x && y<=yCoo.get(i).y)
				return i;
		return -1;
	}
	
	public boolean isValidClick(int x, int y)
	{
		int index = yRangeIndex(y);
		if(index == -1)
			return false;
		
		if(x >= xCoo.get(index) && x <= ((xCoo.get(index)+11*(49+4.7)) - 4.7))
			return true;
		
		return false;
	}
	
}
