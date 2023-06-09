/*
 * OPBM - Office Productivity Benchmark
 *
 * This class was created to allow animations.  It creates a series of ImageIcons
 * which are then associated with a label.  That label is updated repeatedly on
 * timer events, to change its associated ImageIcon, allowing for animation.
 *
 * Last Updated:  Sep 12, 2011
 *
 * by Van Smith
 * Cossatot Analytics Laboratories, LLC. (Cana Labs)
 *
 * (c) Copyright Cana Labs.
 * Free software licensed under the GNU GPL2.
 *
 * @version 1.2.0
 *
 */

package opbm.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import opbm.benchmarks.hud.HUD;

public class AnimateImageTask extends TimerTask
{
	public AnimateImageTask()
	{
		m_images		= new ArrayList<ImageIcon>(0);
		m_activeImage	= 0;
	}

	public void add(ImageIcon img)
	{
		m_images.add(img);
	}

	public void add(AlphaImage img)
	{
		BufferedImage bi = img.getBufferedImage();
		m_images.add(new ImageIcon(bi));
	}

	public void animateAtBounds(int				x,
								int				y,
								int				width,
								int				height,
								JLayeredPane	panel,
								int				interval)
	{
		JLabel j = new JLabel();
		j.setBounds(x, y, width, height);
		panel.add(j);
		panel.moveToFront(j);
		animateComponent(j, interval);
	}

	public void animateComponent(JLabel		label,
								 long		interval)
	{
		m_label	= label;
		m_timer	= new Timer();
		m_timer.schedule(this, 0, interval);
	}

	@Override
	public void run()
	{
		HUD hud;

		if (!m_paused)
		{
			m_label.setIcon(m_images.get(m_activeImage));
			++m_activeImage;

			if (m_activeImage >= m_images.size())
			{
				m_activeImage = 0;
				if (m_callbackObject != null)
				{
					if (m_callbackIdentifier.equalsIgnoreCase("hud"))
					{	// It's a HUD, call its callback
						hud = (HUD)m_callbackObject;
						hud.animateImageTaskCallback(m_callbackParam);
					}
				}
			}
		}
	}

	public void pause()
	{
		m_paused = true;
	}

	public void stop()
	{
		m_paused = true;
		if (m_label != null)
			m_label.setVisible(false);
	}

	public void restart()
	{
		m_paused = false;
		if (m_label != null)
			m_label.setVisible(true);
	}

	public void setupCallback(Object	obj,
							  String	identifier,
							  Object	parameter)
	{
		m_callbackObject		= obj;
		m_callbackIdentifier	= identifier;
		m_callbackParam			= parameter;
	}

	private List<ImageIcon>		m_images;
	private int					m_activeImage;
	private JLabel				m_label;
	private Timer				m_timer;
	private boolean				m_paused;

	// Setup for callback information
	private Object				m_callbackObject;
	private String				m_callbackIdentifier;
	private Object				m_callbackParam;
}
