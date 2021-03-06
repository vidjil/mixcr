/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.mixcr.vdjaligners;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.milaboratory.core.alignment.Aligner;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.AlignmentScoring;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mixcr.basictypes.VDJCHit;
import com.milaboratory.mixcr.reference.Allele;
import com.milaboratory.mixcr.reference.GeneFeature;
import com.milaboratory.mixcr.reference.Locus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public final class SingleDAligner {
    private final AlignmentScoring<NucleotideSequence> scoring;
    private final float absoluteMinScore, relativeMinScore;
    private final int maxHits;
    private final List<SequenceWithLocus> sequences = new ArrayList<>();
    private final List<Allele> alleles;
    private final GeneFeature featureToAlign;

    private final LoadingCache<NucleotideSequence, List<PreVDJCHit>> resultsCache =
            CacheBuilder.newBuilder()
                    .maximumSize(5000)
                    .recordStats()
                    .build(
                            new CacheLoader<NucleotideSequence, List<PreVDJCHit>>() {
                                public List<PreVDJCHit> load(NucleotideSequence key) {
                                    return _align(key);
                                }
                            }
                    );

    public SingleDAligner(DAlignerParameters parameters,
                          List<Allele> alleles) {
        this.scoring = parameters.getScoring();
        this.absoluteMinScore = parameters.getAbsoluteMinScore();
        this.relativeMinScore = parameters.getRelativeMinScore();
        this.maxHits = parameters.getMaxHits();
        this.featureToAlign = parameters.getGeneFeatureToAlign();
        for (Allele allele : alleles)
            sequences.add(new SequenceWithLocus(allele, featureToAlign));
        this.alleles = new ArrayList<>(alleles);
    }

    List<PreVDJCHit> align0(NucleotideSequence sequence, Set<Locus> loci, int from, int to) {
        if (from > to)
            throw new IllegalArgumentException();

        NucleotideSequence key = sequence.getRange(from, to);

        try {
            List<PreVDJCHit> cachedResult = resultsCache.get(key);
            List<PreVDJCHit> result = new ArrayList<>(cachedResult.size());

            PreVDJCHit h;
            for (PreVDJCHit hit : cachedResult) {
                //filter non-possible loci
                if (!loci.contains(sequences.get(hit.id).locus))
                    continue;

                result.add(h = convert(hit, from));

                assert sequence.getRange(h.alignment.getSequence2Range()).equals(
                        h.alignment
                                .getRelativeMutations()
                                .mutate(sequences.get(h.id).sequence
                                        .getRange(h.alignment.getSequence1Range())));
            }

            cutToScore(result);
            return result;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public VDJCHit[] align(NucleotideSequence sequence, Set<Locus> loci, int from, int to,
                           int targetIndex, int numberOfTargets) {
        List<PreVDJCHit> preHits = align0(sequence, loci, from, to);
        return PreVDJCHit.convert(alleles, featureToAlign, preHits,
                targetIndex, numberOfTargets);
    }

    private PreVDJCHit convert(PreVDJCHit hit, int from) {
        Alignment<NucleotideSequence> alignment = hit.alignment;
        return new PreVDJCHit(hit.id, new Alignment<>(alignment.getSequence1(),
                alignment.getAbsoluteMutations(),
                alignment.getSequence1Range(),
                alignment.getSequence2Range().move(from),
                alignment.getScore()));
    }

    private List<PreVDJCHit> _align(NucleotideSequence sequence) {
        if (sequence.size() == 0)
            return Collections.EMPTY_LIST;

        List<PreVDJCHit> result = new ArrayList<>();
        Alignment<NucleotideSequence> alignment;
        for (int i = 0; i < sequences.size(); ++i) {
            alignment = Aligner.alignLocal(scoring, sequences.get(i).sequence, sequence);

            if (alignment == null || alignment.getScore() < absoluteMinScore)
                continue;

            result.add(new PreVDJCHit(i, alignment));
        }

        Collections.sort(result, PreVDJCHit.SCORE_COMPARATOR);
        return result;
    }

    private void cutToScore(List<PreVDJCHit> result) {
        if (result.isEmpty())
            return;
        float threshold = Math.max(absoluteMinScore, result.get(0).alignment.getScore() * relativeMinScore);
        for (int i = result.size() - 1; i >= 0; --i)
            if (result.get(i).alignment.getScore() < threshold
                    || i >= maxHits)
                result.remove(i);
    }

    private static final class SequenceWithLocus {
        private final NucleotideSequence sequence;
        private final Locus locus;

        public SequenceWithLocus(Allele allele, GeneFeature featureToAlign) {
            this.sequence = allele.getFeature(featureToAlign);
            this.locus = allele.getLocus();
        }
    }
}
