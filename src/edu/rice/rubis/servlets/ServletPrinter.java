/*
 * Copyright (C) 2002-2009  OW2 Consortium
 *
 * This file is part of dcsj-rubis (below referred to as "this program").
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.rice.rubis.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletResponse;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 * In fact, this class is not a servlet itself but it provides output services
 * to servlets to send back HTML files.
 * 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a>
 * @author <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class ServletPrinter {
	
	private PrintWriter out;
	private String servletName;
	private GregorianCalendar startDate;
	private static HtmlCleaner htmlCleaner = new HtmlCleaner();

	public ServletPrinter(HttpServletResponse toWebServer,
			String callingServletName) {
		startDate = new GregorianCalendar();
		toWebServer.setContentType("text/html");
		try {
			out = toWebServer.getWriter();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		servletName = callingServletName;
		
		CleanerProperties props = htmlCleaner.getProperties();
		props.setOmitUnknownTags(true);
		props.setOmitDeprecatedTags(true);
		props.setAdvancedXmlEscape(false);
		//TODO: checks if others are required
	}

	public void printFile(String filename) {
		InputStreamReader isr = null;
		try {
			InputStream is = ServletPrinter.class.getClassLoader()
					.getResourceAsStream(filename);
			isr = new InputStreamReader(is);
			// fis = new FileReader(filename);
			char[] data = new char[4 * 1024]; // 4K buffer
			int bytesRead;
			while ((bytesRead = isr.read(data)) != -1) {
				out.write(data, 0, bytesRead);
				bytesRead = isr.read(data);
			}
		} catch (Exception e) {
			out.println("Unable to read file (exception: " + e + ")<br>");
		} finally {
			if (isr != null){
				try {
					isr.close();
				} catch (Exception ex) {
					out.println("Unable to close the file reader (exception: "
							+ ex + ")<br>");
				}
			}
		}
	}

	/**
	 * The file is first read, then processed with some modifications regarding
	 * the current application state {@link Config#loadLevel}.
	 * 
	 * @param filename
	 *            the name of the file to be analyzed and then printed
	 * @throws IOException
	 */
	public void printProcessedFile(String filename) {
		InputStreamReader isr = null;
		try
		{
			InputStream is = ServletPrinter.class.getClassLoader().getResourceAsStream(filename);
			// constructs the dom of the html code
			TagNode root = htmlCleaner.clean(is);
	    	// Processes display policies regarding the load level
		    switch (Config.loadLevel) {
		    case Config.NO_OVERLOAD_LOAD_LEVEL : break;
		    case Config.OVERLOAD_LOAD_LEVEL :
		    	// Removes all img tags using a Html Parser called HtmlCleaner
		    	// http://stackoverflow.com/questions/1699313/how-to-remove-html-tag-in-java
		    	// using regexps is a bad approach (html/regexp have different grammars).
		    	Object[] found = root.evaluateXPath("//img");
		    	if(found != null) {
		    	    for (Object tn : found){
		    	    	if (tn instanceof TagNode)
		    				((TagNode)tn).removeFromTree();
		    	    }
		    	}
		    	break;
		    default: break;
		    }
		    // print the processed file
		    // TODO: shall we use a buffer here or print the whole file from one long single string ?
		    String innerHtml = htmlCleaner.getInnerHtml(root);
		    out.print(innerHtml);
		} catch (Exception e) {
			out.println("Unable to read file (exception: " + e + ")<br>");
		} finally {
			if (isr != null){
				try {
					isr.close();
				} catch (Exception ex) {
					out.println("Unable to close the buffer reader (exception: "
							+ ex + ")<br>");
				}
			}
		}
	}

	void printHTMLheader(String title) {
		printFile(Config.HTMLFilesPath + "/header.html");
		out.println("<title>" + title + "</title>");
	}

	void printHTMLfooter() {
		GregorianCalendar endDate = new GregorianCalendar();

		out.println("<br><hr>RUBiS (C) Rice University/INRIA<br><i>Page generated by "
				+ servletName
				+ " in "
				+ TimeManagement.diffTime(startDate, endDate) + "</i><br>");
		out.println("</body>");
		out.println("</html>");
	}

	void printHTML(String msg) {
		out.println(msg);
	}

	void printHTMLHighlighted(String msg) {
		out.println("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\">");
		out.println("<TR><TD align=\"center\" width=\"100%\"><FONT size=\"4\" color=\"#000000\"><B>"
				+ msg + "</B></FONT></TD></TR>");
		out.println("</TABLE><p>");
	}

	// //////////////////////////////////////
	// Category related printed functions //
	// //////////////////////////////////////

	void printCategory(String categoryName, int categoryId) {
		try {
			out.println("<a href=\"SearchItemsByCategory?category="
					+ categoryId + "&categoryName="
					+ URLEncoder.encode(categoryName, "UTF-8") + "\">"
					+ categoryName + "</a><br>");
		} catch (Exception e) {
			out.println("Unable to print Category (exception: " + e + ")<br>");
		}
	}

	/** List all the categories with links to browse items by region */
	void printCategoryByRegion(String categoryName, int categoryId, int regionId) {
		try {
			out.println("<a href=\"SearchItemsByRegion?category=" + categoryId
					+ "&categoryName="
					+ URLEncoder.encode(categoryName, "UTF-8") + "&region="
					+ regionId + "\">" + categoryName + "</a><br>");
		} catch (Exception e) {
			out.println("Unable to print Category (exception: " + e + ")<br>");
		}
	}

	/** Lists all the categories and links to the sell item page */
	void printCategoryToSellItem(String categoryName, int categoryId, int userId) {
		try {
			out.println("<a href=\"SellItemForm?category=" + categoryId
					+ "&user=" + userId + "\">" + categoryName + "</a><br>");
		} catch (Exception e) {
			out.println("Unable to print Category (exception: " + e + ")<br>");
		}
	}

	// ////////////////////////////////////
	// Region related printed functions //
	// ////////////////////////////////////

	void printRegion(String regionName) {
		try {
			out.println("<a href=\"BrowseCategories?region="
					+ URLEncoder.encode(regionName, "UTF-8") + "\">"
					+ regionName + "</a><br>");
		} catch (Exception e) {
			out.println("Unable to print Region (exception: " + e + ")<br>");
		}
	}

	// ////////////////////////////////////
	// Item related printed functions //
	// ////////////////////////////////////

	void printItemHeader() {
		out.println("<TABLE border=\"1\" summary=\"List of items\">"
				+ "<THEAD>"
				+ "<TR><TH>Designation<TH>Price<TH>Bids<TH>End Date<TH>Bid Now"
				+ "<TBODY>");
	}

	void printItem(String itemName, int itemId, float maxBid, int nbOfBids,
			String endDate) {
		try {
			String img = (Config.loadLevel == Config.OVERLOAD_LOAD_LEVEL ? "<IMG SRC=\"/app/rubis/images/bid_now.jpg\" height=22 width=90>" : "");
			out.println("<TR><TD><a href=\"ViewItem?itemId="
					+ itemId
					+ "\">"
					+ itemName
					+ "<TD>"
					+ maxBid
					+ "<TD>"
					+ nbOfBids
					+ "<TD>"
					+ endDate
					+ "<TD><a href=\"PutBidAuth?itemId="
					+ itemId
					+ "\">" + img + "</a>");
		} catch (Exception e) {
			out.println("Unable to print Item (exception: " + e + ")<br>");
		}
	}

	void printItemFooter() {
		out.println("</TABLE>");
	}

	void printItemFooter(String URLprevious, String URLafter) {
		out.println("</TABLE>\n");
		out.println("<p><CENTER>\n" + URLprevious + "\n&nbsp&nbsp&nbsp"
				+ URLafter + "\n</CENTER>\n");
	}

	/**
	 * Print the full description of an item and the bidding option if userId
	 * &gt; 0.
	 */
	void printItemDescription(int itemId, String itemName, String description,
			float initialPrice, float reservePrice, float buyNow, int quantity,
			float maxBid, int nbOfBids, String sellerName, int sellerId,
			String startDate, String endDate, int userId, Connection conn) {
		PreparedStatement stmt = null;
		try {
			String firstBid;

			// Compute the current price of the item
			if (maxBid == 0) {
				firstBid = "none";
			} else {
				if (quantity > 1) {
					try {
						/*
						 * Get the qty max first bids and parse bids in this
						 * order until qty is reached. The bid that reaches qty
						 * is the current minimum bid.
						 */

						stmt = conn
								.prepareStatement("SELECT id,qty,max_bid FROM bids WHERE item_id=? ORDER BY bid DESC LIMIT ?");
						stmt.setInt(1, itemId);
						stmt.setInt(2, quantity);
						ResultSet rs = stmt.executeQuery();
						if (rs.first()) {
							int numberOfItems = 0;
							int qty;
							do {
								qty = rs.getInt("qty");
								numberOfItems = numberOfItems + qty;
								if (numberOfItems >= quantity) {
									maxBid = rs.getFloat("max_bid");
									break;
								}
							} while (rs.next());
						}
					} catch (Exception e) {
						this.printHTML("Problem while computing current bid: "
								+ e + "<br>");
						if (stmt != null) {
							stmt.close();
						}
						return;
					}
				}
				firstBid = Float.toString(maxBid);
			}
			if (userId > 0) {
				this.printHTMLheader("RUBiS: Bidding\n");
				this.printHTMLHighlighted("You are ready to bid on: "
						+ itemName);
			} else {
				this.printHTMLheader("RUBiS: Viewing " + itemName + "\n");
				this.printHTMLHighlighted(itemName);
			}
			out.println("<TABLE>\n" + "<TR><TD>Currently<TD><b><BIG>" + maxBid
					+ "</BIG></b>\n");
			// Check if the reservePrice has been met (if any)
			if (reservePrice > 0) { // Has the reserve price been met ?
				if (maxBid >= reservePrice) {
					out.println("(The reserve price has been met)\n");
				} else {
					out.println("(The reserve price has NOT been met)\n");
				}
			}
			out.println("<TR><TD>Quantity<TD><b><BIG>"
					+ quantity
					+ "</BIG></b>\n"
					+ "<TR><TD>First bid<TD><b><BIG>"
					+ firstBid
					+ "</BIG></b>\n"
					+ "<TR><TD># of bids<TD><b><BIG>"
					+ nbOfBids
					+ "</BIG></b> (<a href=\"/app/rubis/servlet/ViewBidHistory?itemId="
					+ itemId
					+ "\">bid history</a>)\n"
					+ "<TR><TD>Seller<TD><a href=\"/app/rubis/servlet/ViewUserInfo?userId="
					+ sellerId + "\">" + sellerName
					+ "</a> (<a href=\"/app/rubis/servlet/PutCommentAuth?to="
					+ sellerId + "&itemId=" + itemId
					+ "\">Leave a comment on this user</a>)\n"
					+ "<TR><TD>Started<TD>" + startDate + "\n"
					+ "<TR><TD>Ends<TD>" + endDate + "\n" + "</TABLE>");
			// Can the user buy this item now ?
			if (buyNow > 0) {
				String img = (Config.loadLevel == Config.OVERLOAD_LOAD_LEVEL ? "<IMG SRC=\"/app/rubis/images/buy_it_now.jpg\" height=22 width=150>" : "");
				out.println("<p><a href=\"/app/rubis/servlet/BuyNowAuth?itemId="
						+ itemId
						+ "\">"
						+ img + "</a>"
						+ "  <BIG><b>You can buy this item right now for only $"
						+ buyNow + "</b></BIG><br><p>\n");
			}

			if (userId <= 0) {
				String img = (Config.loadLevel == Config.OVERLOAD_LOAD_LEVEL ? "<IMG SRC=\"/app/rubis/images/bid_now.jpg\" height=22 width=90>" : "");
				
				out.println("<a href=\"/app/rubis/servlet/PutBidAuth?itemId="
						+ itemId
						+ "\">"+ img +" on this item</a>\n");
			}

			this.printHTMLHighlighted("Item description");
			out.println(description);
			out.println("<br><p>\n");

			if (userId > 0) {
				this.printHTMLHighlighted("Bidding");
				float minBid = maxBid + 1;
				this.printHTML("<form action=\"/app/rubis/servlet/StoreBid\" method=POST>\n"
						+ "<input type=hidden name=minBid value="
						+ minBid
						+ ">\n"
						+ "<input type=hidden name=userId value="
						+ userId
						+ ">\n"
						+ "<input type=hidden name=itemId value="
						+ itemId
						+ ">\n"
						+ "<input type=hidden name=maxQty value="
						+ quantity
						+ ">\n"
						+ "<center><table>\n"
						+ "<tr><td>Your bid (minimum bid is "
						+ minBid
						+ "):</td>\n"
						+ "<td><input type=text size=10 name=bid></td></tr>\n"
						+ "<tr><td>Your maximum bid:</td>\n"
						+ "<td><input type=text size=10 name=maxBid></td></tr>\n");
				if (quantity > 1) {
					this.printHTML("<tr><td>Quantity:</td>\n<td><input type=text size=5 name=qty></td></tr>\n");
				} else {
					this.printHTML("<input type=hidden name=qty value=1>\n");
					this.printHTML("</table><p><input type=submit value=\"Bid now!\"></center><p>\n");
				}
			}
		} catch (Exception e) {
			out.println("Unable to print Item description (exception: " + e
					+ ")<br>\n");
		}
	}

	// //////////////////////////////////////
	// About me related printed functions //
	// //////////////////////////////////////

	void printUserBidsHeader() {
		this.printHTMLHighlighted("<p><h3>Items you have bid on.</h3>\n");
		out.println("<TABLE border=\"1\" summary=\"Items You've bid on\">\n"
				+ "<THEAD>\n"
				+ "<TR><TH>Designation<TH>Initial Price<TH>Current price<TH>Your max bid<TH>Quantity"
				+ "<TH>Start Date<TH>End Date<TH>Seller<TH>Put a new bid\n"
				+ "<TBODY>\n");
	}

	void printItemUserHasBidOn(int itemId, String itemName, float initialPrice,
			int quantity, String startDate, String endDate, int sellerId,
			String sellerName, float currentPrice, float maxBid,
			String username, String password) {
		try {
			String img = (Config.loadLevel == Config.OVERLOAD_LOAD_LEVEL ? "<IMG SRC=\"/app/rubis/images/bid_now.jpg\" height=22 width=90>" : "");
			out.println("<TR><TD><a href=\"/app/rubis/servlet/ViewItem?itemId="
					+ itemId
					+ "\">"
					+ itemName
					+ "<TD>"
					+ initialPrice
					+ "<TD>"
					+ currentPrice
					+ "<TD>"
					+ maxBid
					+ "<TD>"
					+ quantity
					+ "<TD>"
					+ startDate
					+ "<TD>"
					+ endDate
					+ "<TD><a href=\"/app/rubis/servlet/ViewUserInfo?userId="
					+ sellerId
					+ "\">"
					+ sellerName
					+ "<TD><a href=\"/app/rubis/servlet/PutBid?itemId="
					+ itemId
					+ "&nickname="
					+ username
					+ "&password="
					+ password
					+ "\">"+img+ "</a>\n");
		} catch (Exception e) {
			out.println("Unable to print Item (exception: " + e + ")<br>\n");
		}
	}

	void printUserWonItemHeader() {
		this.printHTML("<br>");
		this.printHTMLHighlighted("<p><h3>Items you won in the past 30 days.</h3>\n");
		out.println("<TABLE border=\"1\" summary=\"List of items\">\n"
				+ "<THEAD>\n"
				+ "<TR><TH>Designation<TH>Price you bought it<TH>Seller"
				+ "<TBODY>\n");
	}

	void printUserWonItem(int itemId, String itemName, float currentPrice,
			int sellerId, String sellerName) {
		try {
			out.println("<TR><TD><a href=\"/app/rubis/servlet/ViewItem?itemId="
					+ itemId + "\">" + itemName + "</a>\n" + "<TD>"
					+ currentPrice + "\n"
					+ "<TD><a href=\"/app/rubis/servlet/ViewUserInfo?userId="
					+ sellerId + "\">" + sellerName + "</a>\n");
		} catch (Exception e) {
			out.println("Unable to print Item (exception: " + e + ")<br>\n");
		}
	}

	void printUserBoughtItemHeader() {
		this.printHTML("<br>");
		this.printHTMLHighlighted("<p><h3>Items you bouhgt in the past 30 days.</h3>\n");
		out.println("<TABLE border=\"1\" summary=\"List of items\">\n"
				+ "<THEAD>\n"
				+ "<TR><TH>Designation<TH>Quantity<TH>Price you bought it<TH>Seller"
				+ "<TBODY>\n");
	}

	void printUserBoughtItem(int itemId, String itemName, float buyNow,
			int quantity, int sellerId, String sellerName) {
		try {
			out.println("<TR><TD><a href=\"/app/rubis/servlet/ViewItem?itemId="
					+ itemId
					+ "\">"
					+ itemName
					+ "</a>\n"
					+ "<TD>"
					+ quantity
					+ "\n"
					+ "<TD>"
					+ buyNow
					+ "\n"
					+ "<TD><a href=\"/app/rubis/servlet/edu.rice.rubis.beans.servlets.ViewUserInfo?userId="
					+ sellerId + "\">" + sellerName + "</a>\n");
		} catch (Exception e) {
			out.println("Unable to print Item (exception: " + e + ")<br>\n");
		}
	}

	void printSellHeader(String title) {
		this.printHTMLHighlighted("<p><h3>" + title + "</h3>\n");
		out.println("<TABLE border=\"1\" summary=\"List of items\">\n"
				+ "<THEAD>\n"
				+ "<TR><TH>Designation<TH>Initial Price<TH>Current price<TH>Quantity<TH>ReservePrice<TH>Buy Now"
				+ "<TH>Start Date<TH>End Date\n" + "<TBODY>\n");
	}

	void printSell(int itemId, String itemName, float initialPrice,
			float reservePrice, int quantity, float buyNow, String startDate,
			String endDate, float currentPrice) {
		try {
			out.println("<TR><TD><a href=\"/app/rubis/servlet/ViewItem?itemId="
					+ itemId + "\">" + itemName + "<TD>" + initialPrice
					+ "<TD>" + currentPrice + "<TD>" + quantity + "<TD>"
					+ reservePrice + "<TD>" + buyNow + "<TD>" + startDate
					+ "<TD>" + endDate + "\n");
		} catch (Exception e) {
			out.println("Unable to print Item (exception: " + e + ")<br>\n");
		}
	}

	// /////////////////////////////////////
	// Buy now related printed functions //
	// /////////////////////////////////////

	/**
	 * Print the full description of an item and the buy now option
	 * 
	 * @param item
	 *            an <code>Item</code> value
	 * @param userId
	 *            an authenticated user id
	 */
	void printItemDescriptionToBuyNow(int itemId, String itemName,
			String description, float buyNow, int quantity, int sellerId,
			String sellerName, String startDate, String endDate, int userId) {
		try {
			this.printHTMLheader("RUBiS: Buy Now");
			this.printHTMLHighlighted("You are ready to buy this item: "
					+ itemName);

			out.println("<TABLE>\n"
					+ "<TR><TD>Quantity<TD><b><BIG>"
					+ quantity
					+ "</BIG></b>\n"
					+ "<TR><TD>Seller<TD><a href=\"/app/rubis/servlet/ViewUserInfo?userId="
					+ sellerId + "\">" + sellerName
					+ "</a> (<a href=\"/app/rubis/servlet/PutCommentAuth?to="
					+ sellerId + "&itemId=" + itemId
					+ "\">Leave a comment on this user</a>)\n"
					+ "<TR><TD>Started<TD>" + startDate + "\n"
					+ "<TR><TD>Ends<TD>" + endDate + "\n" + "</TABLE>");

			this.printHTMLHighlighted("Item description");
			out.println(description);
			out.println("<br><p>\n");

			this.printHTMLHighlighted("Buy Now");
			printHTML("<form action=\"/app/rubis/servlet/StoreBuyNow\" method=POST>\n"
					+ "<input type=hidden name=userId value="
					+ userId
					+ ">\n"
					+ "<input type=hidden name=itemId value="
					+ itemId
					+ ">\n"
					+ "<input type=hidden name=maxQty value="
					+ quantity
					+ ">\n");
			if (quantity > 1) {
				this.printHTML("<center><table><tr><td>Quantity:</td>\n"
						+ "<td><input type=text size=5 name=qty></td></tr></table></center>\n");
			} else {
				this.printHTML("<input type=hidden name=qty value=1>\n");
			}
			this.printHTML("<p><center><input type=submit value=\"Buy now!\"></center><p>\n");
		} catch (Exception e) {
			out.println("Unable to print Item description (exception: " + e
					+ ")<br>\n");
		}
	}

	// /////////////////////////////////
	// Bid related printed functions //
	// /////////////////////////////////

	void printBidHistoryHeader() {
		out.println("<TABLE border=\"1\" summary=\"List of bids\">" + "<THEAD>"
				+ "<TR><TH>User ID<TH>Bid amount<TH>Date of bid" + "<TBODY>");
	}

	void printBidHistoryFooter() {
		out.println("</TBODY></TABLE>");
	}

	void printBidHistory(int userId, String bidderName, float bid, String date) {
		try {
			out.println("<tr><td><a href=\"viewuserinfo?userid=" + userId
					+ "\">" + bidderName + "<TD>" + bid + "<TD>" + date);
		} catch (Exception e) {
			out.println("Unable to print Bid (exception: " + e + ")<br>");
		}
	}

	// ///////////////////////////////////////
	// Comment related printed functions //
	// ///////////////////////////////////////

	void printCommentHeader() {
		out.println("<DL>");
	}

	void printComment(String userName, int userId, String date, String comment) {
		try {
			out.println("<DT><b><BIG><a href=\"ViewUserInfo?userId=" + userId
					+ "\">" + userName + "</a></BIG></b>" + " wrote the "
					+ date + "<DD><i>" + comment + "</i><p>");
		} catch (Exception e) {
			out.println("Unable to print Comment (exception: " + e + ")<br>");
		}
	}

	void printCommentFooter() {
		out.println("</DL>");
	}
}
