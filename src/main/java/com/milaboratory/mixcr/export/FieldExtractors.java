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
package com.milaboratory.mixcr.export;

import static com.milaboratory.core.sequence.TranslationParameters.FromCenter;

import java.util.ArrayList;
import java.util.List;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.AminoAcidSequence;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mixcr.basictypes.*;
import com.milaboratory.mixcr.reference.GeneType;

public class FieldExtractors extends AbstractFieldExtractors {

	private static FieldExtractors fieldExtractors = new FieldExtractors();

	private FieldExtractors() {}

	public static FieldExtractors getInstance() {
		return fieldExtractors;
	}

    @Override
    public synchronized Field[] getFields() {
        if (descriptors == null) {
            List<Field> desctiptorsList = new ArrayList<>();

            // Number of targets
            desctiptorsList.add(new PL_O("-targets", "Export number of targets", "Number of targets", "numberOfTargets") {
                @Override
                protected String extract(VDJCObject object) {
                    return Integer.toString(object.numberOfTargets());
                }
            });

            // Best hits
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "Hit",
                        "Export best " + l + " hit", "Best " + l + " hit", "best" + l + "Hit") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit bestHit = object.getBestHit(type);
                        if (bestHit == null)
                            return NULL;
                        return bestHit.getAllele().getName();
                    }
                });
            }

            // Best hits
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "HitScore",
                        "Export best score for best " + l + " hit", "Best " + l + " hit score", "best" + l + "HitScore") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit bestHit = object.getBestHit(type);
                        if (bestHit == null)
                            return NULL;
                        return String.valueOf(bestHit.getScore());
                    }
                });
            }

            // All hits
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "HitsWithScore",
                        "Export all " + l + " hits with score", "All " + l + " hits", "all" + l + "HitsWithScore") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit[] hits = object.getHits(type);
                        if (hits.length == 0)
                            return "";
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; ; i++) {
                            sb.append(hits[i].getAllele().getName())
                                    .append("(").append(SCORE_FORMAT.format(hits[i].getScore()))
                                    .append(")");
                            if (i == hits.length - 1)
                                break;
                            sb.append(",");
                        }
                        return sb.toString();
                    }
                });
            }

            // All hits without score
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "Hits",
                        "Export all " + l + " hits", "All " + l + " Hits", "all" + l + "Hits") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit[] hits = object.getHits(type);
                        if (hits.length == 0)
                            return "";
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; ; i++) {
                            sb.append(hits[i].getAllele().getName());
                            if (i == hits.length - 1)
                                break;
                            sb.append(",");
                        }
                        return sb.toString();
                    }
                });
            }

            // Best alignment
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "Alignment",
                        "Export best " + l + " alignment", "Best " + l + " alignment", "best" + l + "Alignment") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit bestHit = object.getBestHit(type);
                        if (bestHit == null)
                            return NULL;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; ; i++) {
                            Alignment<NucleotideSequence> alignment = bestHit.getAlignment(i);
                            if (alignment == null)
                                sb.append(NULL);
                            else
                                sb.append(alignment.toCompactString());
                            if (i == object.numberOfTargets() - 1)
                                break;
                            sb.append(",");
                        }
                        return sb.toString();
                    }
                });
            }

            // All alignments
            for (final GeneType type : GeneType.values()) {
                char l = type.getLetter();
                desctiptorsList.add(new PL_O("-" + Character.toLowerCase(l) + "Alignments",
                        "Export all " + l + " alignments", "All " + l + " alignments", "all" + l + "Alignments") {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit[] hits = object.getHits(type);
                        if (hits.length == 0)
                            return "";
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; ; ++j) {
                            for (int i = 0; ; i++) {
                                Alignment<NucleotideSequence> alignment = hits[j].getAlignment(i);
                                if (alignment == null)
                                    sb.append(NULL);
                                else
                                    sb.append(alignment.toCompactString());
                                if (i == object.numberOfTargets() - 1)
                                    break;
                                sb.append(',');
                            }
                            if (j == hits.length - 1)
                                break;
                            sb.append(';');
                        }
                        return sb.toString();
                    }
                });
            }

            desctiptorsList.add(new FeatureExtractorDescriptor("-nFeature", "Export nucleotide sequence of specified gene feature", "N. Seq.", "nSeq") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return seq.getSequence().toString();
                }
            });

            desctiptorsList.add(new FeatureExtractorDescriptor("-qFeature", "Export quality string of specified gene feature", "Qual.", "qual") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return seq.getQuality().toString();
                }
            });

            desctiptorsList.add(new FeatureExtractorDescriptor("-aaFeature", "Export amino acid sequence of specified gene feature", "AA. Seq.", "aaSeq") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return AminoAcidSequence.translate(seq.getSequence(), FromCenter).toString();
                }
            });

            desctiptorsList.add(new FeatureExtractorDescriptor("-minFeatureQuality", "Export minimal quality of specified gene feature", "Min. qual.", "minQual") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return "" + seq.getQuality().minValue();
                }
            });

            desctiptorsList.add(new FeatureExtractorDescriptor("-avrgFeatureQuality", "Export average quality of specified gene feature", "Mean. qual.", "meanQual") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return "" + seq.getQuality().meanValue();
                }
            });

            desctiptorsList.add(new FeatureExtractorDescriptor("-lengthOf", "Exports length of specified gene feature.", "Length of ", "lengthOf") {
                @Override
                public String convert(NSequenceWithQuality seq) {
                    return "" + seq.size();
                }
            });

            desctiptorsList.add(new ExtractReferencePointPosition());

            desctiptorsList.add(new ExtractDefaultReferencePointsPositions());

            desctiptorsList.add(new PL_A("-readId", "Export id of read corresponding to alignment", "Read id", "readId") {
                @Override
                protected String extract(VDJCAlignments object) {
                    return "" + object.getReadId();
                }
            });

            desctiptorsList.add(new ExtractSequence(VDJCAlignments.class, "-sequence",
                    "Export aligned sequence (initial read), or 2 sequences in case of paired-end reads",
                    "Read(s) sequence", "readSequence"));

            desctiptorsList.add(new ExtractSequenceQuality(VDJCAlignments.class, "-quality",
                    "Export initial read quality, or 2 qualities in case of paired-end reads",
                    "Read(s) sequence qualities", "readQuality"));

            desctiptorsList.add(new PL_C("-cloneId", "Unique clone identifier", "Clone ID", "cloneId") {
                @Override
                protected String extract(Clone object) {
                    return "" + object.getId();
                }
            });

            desctiptorsList.add(new PL_C("-count", "Export clone count", "Clone count", "cloneCount") {
                @Override
                protected String extract(Clone object) {
                    return "" + object.getCount();
                }
            });

            desctiptorsList.add(new PL_C("-fraction", "Export clone fraction", "Clone fraction", "cloneFraction") {
                @Override
                protected String extract(Clone object) {
                    return "" + object.getFraction();
                }
            });

            desctiptorsList.add(new ExtractSequence(Clone.class, "-sequence",
                    "Export aligned sequence (initial read), or 2 sequences in case of paired-end reads",
                    "Clonal sequence(s)", "clonalSequence"));

            desctiptorsList.add(new ExtractSequenceQuality(Clone.class, "-quality",
                    "Export initial read quality, or 2 qualities in case of paired-end reads",
                    "Clonal sequence quality(s)", "clonalSequenceQuality"));

            desctiptorsList.add(new PL_A("-descrR1", "Export description line from initial .fasta or .fastq file " +
                    "of the first read (only available if --save-description was used in align command)", "Description R1", "descrR1") {
                @Override
                protected String extract(VDJCAlignments object) {
                    String[] ds = object.getDescriptions();
                    if (ds == null || ds.length == 0)
                        throw new IllegalArgumentException("Error for option \'-descrR1\':\n" +
                                "No description available for read: either re-run align action with --save-description option " +
                                "or don't use \'-descrR1\' in exportAlignments");
                    return ds[0];
                }
            });

            desctiptorsList.add(new PL_A("-descrR2", "Export description line from initial .fasta or .fastq file " +
                    "of the second read (only available if --save-description was used in align command)", "Description R2", "descrR2") {
                @Override
                protected String extract(VDJCAlignments object) {
                    String[] ds = object.getDescriptions();
                    if (ds == null || ds.length < 2)
                        throw new IllegalArgumentException("Error for option \'-descrR2\':\n" +
                                "No description available for second read: either re-run align action with --save-description option " +
                                "or don't use \'-descrR2\' in exportAlignments");
                    return ds[1];
                }
            });

            desctiptorsList.add(alignmentsToClone("-cloneId", "To which clone alignment was attached.", false));
            desctiptorsList.add(alignmentsToClone("-cloneIdWithMappingType", "To which clone alignment was attached with additional info on mapping type.", true));
            desctiptorsList.add(new AbstractField<Clone>(Clone.class, "-readIds", "Read IDs aggregated by clone.") {
                @Override
                public FieldExtractor<Clone> create(OutputMode outputMode, String[] args) {
                    return new CloneToReadsExtractor(outputMode, args[0]);
                }
            });

            for (final GeneType type : GeneType.values()) {
                String c = Character.toLowerCase(type.getLetter()) + "IdentityPercents";
                desctiptorsList.add(new PL_O("-" + c, type.getLetter() + " alignment identity percents",
                        type.getLetter() + " alignment identity percents", c) {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit[] hits = object.getHits(type);
                        if (hits == null)
                            return NULL;
                        StringBuilder sb = new StringBuilder();
                        sb.append("");
                        for (int i = 0; ; i++) {
                            sb.append(hits[i].getIdentity());
                            if (i == hits.length - 1)
                                return sb.toString();
                            sb.append(",");
                        }
                    }
                });
            }
            for (final GeneType type : GeneType.values()) {
                String c = Character.toLowerCase(type.getLetter()) + "BestIdentityPercent";
                desctiptorsList.add(new PL_O("-" + c, type.getLetter() + "best alignment identity percent",
                        type.getLetter() + "best alignment identity percent", c) {
                    @Override
                    protected String extract(VDJCObject object) {
                        VDJCHit hit = object.getBestHit(type);
                        if (hit == null)
                            return NULL;
                        return Float.toString(hit.getIdentity());
                    }
                });
            }
            descriptors = desctiptorsList.toArray(new Field[desctiptorsList.size()]);
        }

        return descriptors;
    }

    protected class ExtractDefaultReferencePointsPositions extends AbstractExtractDefaultReferencePointsPositions {
        public ExtractDefaultReferencePointsPositions() {
            super();
        }
    }

    protected class ExtractSequence extends AbstractExtractSequence {
        protected ExtractSequence(Class targetType, String command, String description, String hHeader, String sHeader) {
            super(targetType, command, description, hHeader, sHeader);
        }

        @Override
        protected String extract(VDJCObject object) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; ; i++) {
                sb.append(object.getTarget(i).getSequence());
                if (i == object.numberOfTargets() - 1)
                    break;
                sb.append(",");
            }
            return sb.toString();
        }
    }

}
