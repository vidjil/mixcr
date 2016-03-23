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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.AminoAcidSequence;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mixcr.basictypes.Clone;
import com.milaboratory.mixcr.basictypes.SequencePartitioning;
import com.milaboratory.mixcr.basictypes.VDJCAlignments;
import com.milaboratory.mixcr.basictypes.VDJCHit;
import com.milaboratory.mixcr.basictypes.VDJCObject;
import com.milaboratory.mixcr.reference.GeneType;
import com.milaboratory.mixcr.reference.ReferencePoint;

public class VidjilFieldExtractors extends AbstractFieldExtractors {


	private static VidjilFieldExtractors fieldExtractors = new VidjilFieldExtractors();

	private VidjilFieldExtractors() {}

	public static VidjilFieldExtractors getInstance() {
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
            desctiptorsList.add(new PL_O("-name",
                    "Name of VDJC", "Name", "name") {
                @Override
                protected String extract(VDJCObject object) {
                    JSONObject jObj = new JSONObject();
                	StringBuilder sb = new StringBuilder();
                	String prefix = "";
                	for (final GeneType type : GeneType.values()) {
                		VDJCHit bestHit = object.getBestHit(type);
                		if (bestHit != null) {
                		    sb.append(prefix);
                		    prefix = ", ";
                		    sb.append(bestHit.getAllele().getName());
                		}
                	}
                	jObj.put(sHeader, sb.toString());
                	return jObj.toJSONString();
                }
            });

            desctiptorsList.add(new PL_C("-top", "Top clone ranking", "Top", "top") {
                @Override
                protected String extract(Clone object) {
                    JSONObject jObj = new JSONObject();
                    jObj.put(this.sHeader, new Integer(object.getId()) + 1);
                    return jObj.toString();
                }
            });

            // TODO not quite the same in vidjil
            desctiptorsList.add(new PL_C("-reads", "Export reads count", "Reads", "reads") {
                @Override
                protected String extract(Clone object) {
                    JSONObject jObj = new JSONObject();
                    List<Long> counts = new ArrayList<>();
                    counts.add(object.getCount());
                    jObj.put(this.sHeader, counts);
                    return jObj.toString();
                }
            });

            desctiptorsList.add(new PL_C("-fraction", "Export clone fraction", "Clone fraction", "cloneFraction") {
                @Override
                protected String extract(Clone object) {
                    JSONObject jObj = new JSONObject();
                    jObj.put(this.sHeader, object.getFraction());
                    return jObj.toJSONString();
                }
            });

            desctiptorsList.add(new PL_C("-germline", "Export clone germline (locus)", "Germline", "germline") {
                @Override
                protected String extract(Clone object) {
                    VDJCHit bestHit = null;
                    int iter = 0;
                    GeneType[] geneTypes = GeneType.values();
                    while(bestHit == null && iter < geneTypes.length) {
                        bestHit = object.getBestHit(geneTypes[iter]);
                        iter++;
                    }
                    JSONObject jObj = new JSONObject();
                    if (bestHit != null) {
                        jObj.put(this.sHeader, bestHit.getAllele().getGeneGroup().getLocus().getId());
                    }
                    return jObj.toString();
                }
            });

            desctiptorsList.add(new ExtractSequence(Clone.class, "-id",
                    "Export aligned sequence (initial read), or 2 sequences in case of paired-end reads",
                    "ID", "id"));

            desctiptorsList.add(new ExtractSequenceQuality(Clone.class, "-quality",
                    "Export initial read quality, or 2 qualities in case of paired-end reads",
                    "Clonal sequence quality(s)", "clonalSequenceQuality"));

            desctiptorsList.add(new ExtractFullSequence(Clone.class, "-sequence",
                    "Export full original sequence", "Sequence", "sequence"));

            desctiptorsList.add(new ExtractSegmentationDetails());

            descriptors = desctiptorsList.toArray(new Field[desctiptorsList.size()]);
        }

        return descriptors;
    }

    protected class ExtractSegmentationDetails extends AbstractExtractDefaultReferencePointsPositions {
        public ExtractSegmentationDetails() {
            super("-seg", "Outputs a list of default reference points (like CDR2Begin, FR4End, etc. " +
                    "see documentation for the full list and formatting)", "Segmentation", "seg");
        }

        @Override
        protected String extract(VDJCObject object) {
            Map<String, Integer> pos = new HashMap<>();
            Map<String, String> jsonMap = new HashMap<>();

            for (final GeneType type : GeneType.values()) {
                VDJCHit bestHit = object.getBestHit(type);
                String identifier = "";
                if (bestHit != null) {
                    Integer QTo = null;
                    Integer QFrom = null;
                    for (int i = 0; ; i++) {
                        Alignment<NucleotideSequence> alignment = bestHit.getAlignment(i);
                        if (alignment != null) {
                            QTo  = alignment.getSequence1Range().getTo();
                            QFrom = alignment.getSequence1Range().getFrom();
                        }
                        if (i == object.numberOfTargets() - 1)
                            break;
                    }

                    switch (type) {
                    case Variable:
                        identifier = "5";
                        if(QTo != null)
                            jsonMap.put("5", "{stop: " + QTo.toString() + "}");
                        break;
                    case Diversity:
                        identifier = "4";
                        break;
                    case Joining:
                        identifier = "3";
                        if(QFrom != null)
                            jsonMap.put("3", "{start: " + QFrom.toString() + "}");
                        break;
                    default:
                        break;
                    }
                    jsonMap.put(identifier, bestHit.getAllele().getName());
                }
            }

            JSONObject seg = new JSONObject(jsonMap);
            JSONObject segWrapper = new JSONObject();
            segWrapper.put(this.sHeader, seg);
            return segWrapper.toJSONString();
        }
    }

    protected class ExtractSequence extends AbstractExtractSequence {
        public ExtractSequence (Class targetType, String command, String description, String hHeader,
                    String sHeader) {
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
            sb.append(" ");
            sb.append(object.getBestHit(GeneType.Variable).getAllele().getName());
            sb.append(" ");
            sb.append(object.getBestHit(GeneType.Joining).getAllele().getName());
            JSONObject jObj = new JSONObject();
            jObj.put(this.sHeader, sb.toString());
            return jObj.toJSONString();
        }
    }

    protected class ExtractFullSequence extends AbstractExtractSequence {
        public ExtractFullSequence (Class targetType, String command, String description, String hHeader,
                    String sHeader) {
                super(targetType, command, description, hHeader, sHeader);
            }

        @Override
        protected String extract(VDJCObject object) {
            StringBuilder sb = new StringBuilder();

            String prefix = "";
            for(NucleotideSequence n : ((Clone) object).originalSequences) {
                sb.append(prefix);
                prefix = ",";
                sb.append(n.toString());
            }


            JSONObject jObj = new JSONObject();
            jObj.put(this.sHeader, sb.toString());
            return jObj.toJSONString();
        }
    }

}
