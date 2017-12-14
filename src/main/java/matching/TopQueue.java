package matching;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

/**
 * This is a priority queue with a fixed capacity to store candidate results in increasing order of score,
 * such as the head of the queue is the last top k candidate, the one with the lowest score and the next to
 * be removed once a new candidate is inserted in a full queue.
 * 
 * @author Nicola Tonellotto
 */
public class TopQueue 
{
	private static final int INTERNAL_MIN_QUEUE_SIZE = 20;
	private PriorityQueue<Result> queue = null;
	private int k;
	
	/* The minimum score for insertion in a queue that is not yet full */
	private float threshold;
	
	public TopQueue(final int k)
	{
		this(k, 0.0f);
	}
	
	public TopQueue(final int k, final float th)
	{
		this.k = k > 0 ? k : 1;;
		this.queue = this.k <= INTERNAL_MIN_QUEUE_SIZE ? new ObjectArrayPriorityQueue<Result>(this.k) : new ObjectHeapPriorityQueue<Result>(this.k);
		// this.threshold = th > 0.0f ? th : 0.0f;
		this.threshold = th;
	}
	
	public boolean insert(Result c)
	{
		if (c.getScore() > threshold) { // if c must enter
			if (queue.size() >= k) // if we have no space
				queue.dequeue();
			queue.enqueue(c); // c enters
			if (queue.size() >= k)
				threshold = Math.max(threshold, queue.first().getScore()); // threshold is updated if queue is full
			return true;
		}
		return false;
	}
	
	public float getTopScore()
	{
		return threshold;
	}
	
	public boolean wouldEnter(Result c)
	{
		return c.getScore() > threshold;
	}

	public boolean wouldEnter(float score)
	{
		return score > threshold;
	}

	
	public PriorityQueue<Result> getTop()
	{
		return this.queue;
	}
	
	public int size()
	{
		return queue.size();
	}
	
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
	
	public void clear()
	{
		queue.clear();
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer("< " + queue.size() + " items, " + getTopScore() + ">");
		return buf.toString();
	}
}
