package org.usf.inspect.server;

import static org.usf.inspect.server.Utils.isEmpty;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class TreeIterator<T,E> implements Iterator<E> {
	
	private final Iterator<T> prt;
	private final Function<T, ? extends Collection<E>> fn;
	private Iterator<E> itr = new Iterator<>() {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public E next() {
			throw new IllegalStateException("invoke hasNext");
		}
	};
	
	@Override
	public boolean hasNext() {
		if(itr.hasNext()) {
			return true;
		}
		Collection<E> c = null;
		while(prt.hasNext() && isEmpty(c = fn.apply(prt.next())));
		if(!isEmpty(c)) {
			itr = c.iterator();
			return itr.hasNext();
		}
		return false;
	}

	@Override
	public E next() {
		return itr.next();
	}
	
	public static <T,E> Iterator<E> treeIterator(Collection<T> list, Function<T, ? extends Collection<E>> fn){
		return new TreeIterator<>(list.iterator(), fn);
	}

	public static <T,U,E> Iterator<E> treeIterator(Collection<T> list, Function<T, ? extends Collection<U>> fn, Function<U, ? extends Collection<E>> fn2){
		return new TreeIterator<>(new TreeIterator<>(list.iterator(), fn), fn2);
	}
}
