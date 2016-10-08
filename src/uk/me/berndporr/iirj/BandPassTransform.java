package uk.me.berndporr.iirj;

import org.apache.commons.math3.complex.Complex;

public class BandPassTransform {

	double wc2;
	double wc;
	double a, b;
	double a2, b2;
	double ab, ab_2;

	BandPassTransform(double fc, double fw, LayoutBase digital,
			LayoutBase analog) {

		digital.reset();

		double ww = 2 * Math.PI * fw;

		// pre-calcs
		wc2 = 2 * Math.PI * fc - (ww / 2);
		wc = wc2 + ww;

		// what is this crap?
		if (wc2 < 1e-8)
			wc2 = 1e-8;
		if (wc > Math.PI - 1e-8)
			wc = Math.PI - 1e-8;

		a = Math.cos((wc + wc2) * 0.5) / Math.cos((wc - wc2) * 0.5);
		b = 1 / Math.tan((wc - wc2) * 0.5);
		a2 = a * a;
		b2 = b * b;
		ab = a * b;
		ab_2 = 2 * ab;

		int numPoles = analog.getNumPoles();
		int pairs = numPoles / 2;
		for (int i = 0; i < pairs; ++i) {
			PoleZeroPair pair = analog.getPair(i);
			ComplexPair p1 = transform(pair.poles.first);
			ComplexPair z1 = transform(pair.zeros.first);

			digital.addPoleZeroConjugatePairs(p1.first, z1.first);
			digital.addPoleZeroConjugatePairs(p1.second, z1.second);
		}

		if ((numPoles & 1) == 1) {
			ComplexPair poles = transform(analog.getPair(pairs).poles.first);
			ComplexPair zeros = transform(analog.getPair(pairs).zeros.first);

			digital.add(poles, zeros);
		}

		double wn = analog.getNormalW();
		digital.setNormal(
				2 * Math.atan(Math.sqrt(Math.tan((wc + wn) * 0.5)
						* Math.tan((wc2 + wn) * 0.5))), analog.getNormalGain());
	}

	ComplexPair transform(Complex c) {
		if (c.isInfinite()) {
			return new ComplexPair(new Complex(-1), new Complex(1));
		}

		c = ((new Complex(1)).add(c)).divide((new Complex(1)).subtract(c)); // bilinear

		Complex v = new Complex(0);
		v = MathSupplement.addmul(v, 4 * (b2 * (a2 - 1) + 1), c);
		v = v.add(8 * (b2 * (a2 - 1) - 1));
		v = v.multiply(c);
		v = v.add(4 * (b2 * (a2 - 1) + 1));
		v = v.sqrt();

		Complex u = v.multiply(-1);
		u = MathSupplement.addmul(u, ab_2, c);
		u = u.add(ab_2);

		v = MathSupplement.addmul(v, ab_2, c);
		v = v.add(ab_2);

		Complex d = new Complex(0);
		d = MathSupplement.addmul(d, 2 * (b - 1), c).add(2 * (1 + b));

		return new ComplexPair(u.divide(d), v.divide(d));
	}

}
